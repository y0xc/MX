use crate::search::ValueType;

#[repr(packed)]
#[derive(Debug, Clone, Copy)]
pub struct FuzzySearchResultItem {
    pub address: u64,
    pub value: FuzzyValue,
}

impl FuzzySearchResultItem {
    pub fn typ(&self) -> ValueType {
        match self.value { 
            FuzzyValue::I8(_) => ValueType::Byte,
            FuzzyValue::I16(_) => ValueType::Word,
            FuzzyValue::I32(_) => ValueType::Dword,
            FuzzyValue::I64(_) => ValueType::Qword,
            FuzzyValue::I128(_) => ValueType::Qword, // No direct mapping
            FuzzyValue::F32(_) => ValueType::Float,
            FuzzyValue::F64(_) => ValueType::Double,
            FuzzyValue::U8(_) => ValueType::Byte,
            FuzzyValue::U16(_) => ValueType::Word,
            FuzzyValue::U32(_) => ValueType::Dword,
            FuzzyValue::U64(_) => ValueType::Qword,
            FuzzyValue::U128(_) => ValueType::Qword, // No direct mapping
        }
    }
}

#[derive(Debug, Clone, Copy)]
pub enum FuzzyValue {
    I8(i8),
    I16(i16),
    I32(i32),
    I64(i64),
    I128(i128),
    F32(f32),
    F64(f64),
    U8(u8),
    U16(u16),
    U32(u32),
    U64(u64),
    U128(u128),
}

macro_rules! impl_fuzzy_from {
    ($($rust_type:ty => $variant:ident),* $(,)?) => {
        $(
            impl From<(u64, $rust_type)> for FuzzySearchResultItem {
                fn from(tuple: (u64, $rust_type)) -> Self {
                    FuzzySearchResultItem {
                        address: tuple.0,
                        value: FuzzyValue::$variant(tuple.1),
                    }
                }
            }
        )*
    };
}

impl_fuzzy_from! {
    i8 => I8,
    i16 => I16,
    i32 => I32,
    i64 => I64,
    i128 => I128,
    f32 => F32,
    f64 => F64,
    u8 => U8,
    u16 => U16,
    u32 => U32,
    u64 => U64,
    u128 => U128,
}


