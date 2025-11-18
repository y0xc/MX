#![allow(E0133)]
#![allow(unsafe_code)]

use crate::ext::jni::{JniResult, JniResultExt};
use crate::wuwa::{BindProc, WuWaDriver, WuwaMemoryType};
use anyhow::anyhow;
use jni::JNIEnv;
use jni::objects::{JObject, JString};
use jni::strings::JNIString;
use jni::sys::{JNI_FALSE, JNI_TRUE, jboolean};
use jni_macro::jni_method;
use lazy_static::lazy_static;
use log::{debug, error, info};
use obfstr::obfstr as s;
use std::sync::RwLock;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum MemoryAccessMode {
    None,
    NonCacheable,
    WriteThrough,
    Normal,
    PageFault,
}

impl MemoryAccessMode {
    #[inline]
    pub fn from_id(id: i32) -> Option<Self> {
        match id {
            0 => Some(MemoryAccessMode::None),
            1 => Some(MemoryAccessMode::NonCacheable),
            2 => Some(MemoryAccessMode::WriteThrough),
            3 => Some(MemoryAccessMode::Normal),
            4 => Some(MemoryAccessMode::PageFault),
            _ => None,
        }
    }
}

impl Into<WuwaMemoryType> for MemoryAccessMode {
    fn into(self) -> WuwaMemoryType {
        match self {
            MemoryAccessMode::None => WuwaMemoryType::Normal,
            MemoryAccessMode::NonCacheable => WuwaMemoryType::DeviceNGnRnE,
            MemoryAccessMode::WriteThrough => WuwaMemoryType::NormalWt,
            MemoryAccessMode::Normal => WuwaMemoryType::Normal,
            MemoryAccessMode::PageFault => WuwaMemoryType::Normal,
        }
    }
}

pub struct DriverManager {
    driver: Option<WuWaDriver>,
    bound_process: Option<BindProc>,
    bound_pid: i32,
    access_mode: MemoryAccessMode,
}

impl DriverManager {
    pub fn new() -> Self {
        Self {
            driver: None,
            bound_process: None,
            bound_pid: 0,
            access_mode: MemoryAccessMode::None,
        }
    }

    pub fn set_driver(&mut self, driver: WuWaDriver) {
        self.driver = Some(driver);
    }

    pub fn get_driver(&self) -> Option<&WuWaDriver> {
        self.driver.as_ref()
    }

    pub fn is_driver_loaded(&self) -> bool {
        self.driver.is_some()
    }

    pub fn set_access_mode(&mut self, mode: MemoryAccessMode) {
        self.access_mode = mode;
        if self.is_process_bound() {
            if let Some(bind_proc) = &self.bound_process {
                if let Err(e) = bind_proc.set_memory_type(self.access_mode.into()) {
                    error!("Failed to update memory type for bound process: {:?}", e);
                }
            }
        }
    }

    pub fn get_access_mode(&self) -> MemoryAccessMode {
        self.access_mode
    }

    pub fn bind_process(&mut self, bind_proc: BindProc, pid: i32) -> anyhow::Result<()> {
        bind_proc.set_memory_type(self.access_mode.into())?;
        self.bound_process = Some(bind_proc);
        self.bound_pid = pid;
        Ok(())
    }

    pub fn unbind_process(&mut self) {
        self.bound_process = None;
        self.bound_pid = 0;
    }

    pub fn is_process_bound(&self) -> bool {
        self.bound_process.is_some() && self.bound_pid != 0
    }

    pub fn get_bound_pid(&self) -> i32 {
        self.bound_pid
    }

    pub fn get_bound_process(&self) -> Option<&BindProc> {
        self.bound_process.as_ref()
    }
}

lazy_static! {
    pub static ref DRIVER_MANAGER: RwLock<DriverManager> = RwLock::new(DriverManager::new());
}

#[jni_method(90, "moe/fuqiuluo/mamu/MamuApplication", "initMamuCore", "()Z")]
pub fn jni_init_core(mut env: JNIEnv, obj: JObject) -> jboolean {
    (|| -> JniResult<jboolean> {
        rayon::ThreadPoolBuilder::new().num_threads(8).build_global()?;

        let package_name = env
            .call_method(&obj, s!("getPackageName"), s!("()Ljava/lang/String;"), &[])?
            .l()?;
        let package_name = JString::from(package_name);
        let package_name_str: String = env.get_string(&package_name)?.into();

        // 这里做一个简单的包名验证，确保只在指定包名下初始化
        if package_name_str != s!("moe.fuqiuluo.mamu") {
            env.throw(s!("Invalid package name for Mamu core initialization"))?;
            return Ok(JNI_FALSE);
        }

        info!("{}: {}", s!("初始化Mamu核心成功，包名"), package_name_str);

        Ok(JNI_TRUE)
    })()
    .or_throw(&mut env)
}

#[jni_method(90, "moe/fuqiuluo/mamu/flutter/FloatingBridge", "setDriverFd", "(I)Z")]
pub fn jni_set_driver_fd(mut env: JNIEnv, _obj: JObject, fd: i32) -> jboolean {
    (|| -> JniResult<jboolean> {
        if DRIVER_MANAGER.is_poisoned() {
            return Err(anyhow!("DriverManager is poisoned"));
        }

        let mut manager = DRIVER_MANAGER
            .write()
            .map_err(|_| anyhow!("Failed to acquire DriverManager write lock"))?;

        if !manager.is_driver_loaded() {
            manager.set_driver(WuWaDriver::from_fd(fd));
            debug!("{}: {}, {}", s!("设置驱动文件描述符"), fd, s!("驱动已初始化"));
        }

        if let Some(driver) = manager.get_driver() {
            let Ok(proc_info) = (unsafe { driver.get_process_info(nix::libc::getpid()) }) else {
                return Err(anyhow!("Failed to get process info"));
            };
            let split_index = proc_info
                .name
                .iter()
                .position(|&c| c == 0)
                .unwrap_or(proc_info.name.len());
            let cmdline = String::from_utf8(proc_info.name[0..split_index].to_vec()).unwrap_or_default();
            if !cmdline.contains(s!("fuqiuluo")) {
                return Err(anyhow!("Current process name verification failed"));
            }

            debug!("{}: {}", s!("驱动初始化成功，当前进程名称"), cmdline);
        } else {
            return Err(anyhow!("Failed to initialize driver"));
        }

        Ok(JNI_TRUE)
    })()
    .or_throw(&mut env)
}

#[jni_method(90, "moe/fuqiuluo/mamu/driver/WuwaDriver", "nativeIsLoaded", "()Z")]
pub fn jni_is_loaded(_env: JNIEnv, _obj: JObject) -> jboolean {
    if let Ok(manager) = DRIVER_MANAGER.read() {
        if manager.is_driver_loaded() {
            JNI_TRUE
        } else {
            JNI_FALSE
        }
    } else {
        JNI_FALSE
    }
}

#[jni_method(90, "moe/fuqiuluo/mamu/driver/WuwaDriver", "nativeSetMemoryAccessMode", "(I)V")]
pub fn jni_set_memory_access_mode(mut env: JNIEnv, _obj: JObject, mode_id: i32) {
    (|| -> JniResult<()> {
        if DRIVER_MANAGER.is_poisoned() {
            return Err(anyhow!("DriverManager is poisoned"));
        }
        let mut manager = DRIVER_MANAGER
            .write()
            .map_err(|_| anyhow!("Failed to acquire DriverManager write lock"))?;
        let mode =
            MemoryAccessMode::from_id(mode_id).ok_or_else(|| anyhow!("Invalid memory access mode id: {}", mode_id))?;
        manager.set_access_mode(mode);
        debug!("{}: {}, {}", s!("设置内存访问模式"), mode_id, format!("{:?}", mode));
        Ok(())
    })()
    .or_throw(&mut env)
}
