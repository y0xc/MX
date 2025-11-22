//! Refine search (filter) tests
//! Tests various combinations of initial search and refine operations

#[cfg(test)]
mod tests {
    use crate::search::engine::ValuePair;
    use crate::search::tests::mock_memory::MockMemory;
    use crate::search::{BPLUS_TREE_ORDER, SearchEngineManager, SearchMode, SearchQuery, SearchValue, ValueType};
    use crate::wuwa::PageStatusBitmap;
    use anyhow::Result;
    use bplustree::BPlusTreeSet;
    use std::collections::{BTreeSet, HashSet};

    /// Helper function to perform a single value search
    fn perform_single_search(
        mem: &MockMemory,
        search_value: &SearchValue,
        base_addr: u64,
        region_size: usize,
    ) -> Result<BPlusTreeSet<ValuePair>> {
        let mut results = BPlusTreeSet::new(BPLUS_TREE_ORDER);
        let mut matches_checked = 0usize;

        let chunk_size = 64 * 1024;
        let mut current = base_addr;
        let end_addr = base_addr + region_size as u64;
        let value_type = search_value.value_type();

        while current < end_addr {
            let chunk_end = (current + chunk_size as u64).min(end_addr);
            let chunk_len = (chunk_end - current) as usize;

            let mut chunk_buffer = vec![0u8; chunk_len];
            let mut page_status = PageStatusBitmap::new(chunk_len, current as usize);

            if mem
                .mem_read_with_status(current, &mut chunk_buffer, &mut page_status)
                .is_ok()
            {
                SearchEngineManager::search_in_buffer_with_status(
                    &chunk_buffer,
                    current,
                    base_addr,
                    end_addr,
                    value_type.size(),
                    search_value,
                    value_type,
                    &page_status,
                    &mut results,
                    &mut matches_checked,
                );
            }

            current = chunk_end;
        }

        Ok(results)
    }

    /// Helper function to simulate refine search on existing results
    fn refine_results_single(
        mem: &MockMemory,
        existing_results: &BPlusTreeSet<ValuePair>,
        search_value: &SearchValue,
    ) -> Result<Vec<ValuePair>> {
        let mut refined_results = Vec::with_capacity(existing_results.len());

        for pair in existing_results.iter() {
            let addr = pair.addr;
            let buffer_size = pair.value_type.size();

            if let Ok(buffer) = mem.mem_read(addr, buffer_size) {
                if let Ok(true) = search_value.matched(&buffer) {
                    refined_results.push(pair.clone());
                }
            }
        }

        Ok(refined_results)
    }

    /// Benchmark version of refine_results_single with detailed timing
    ///
    /// # Performance Optimization
    /// Uses `Vec` instead of `BPlusTreeSet` for result storage because:
    /// - Input from `existing_results.iter()` is already sorted (B+tree iteration is ordered)
    /// - Sequential insertion preserves order naturally
    /// - `Vec::push` is O(1) amortized vs B+tree insert O(log n)
    /// - Pre-allocation with `with_capacity` eliminates reallocation overhead
    fn refine_results_single_with_benchmark(
        mem: &MockMemory,
        existing_results: &BPlusTreeSet<ValuePair>,
        search_value: &SearchValue,
    ) -> Result<(Vec<ValuePair>, BenchmarkStats)> {
        use std::time::Instant;

        // Pre-allocate capacity for best-case scenario (all results match)
        let mut refined_results = Vec::with_capacity(existing_results.len());

        // Timing accumulators
        let mut total_iteration_time = std::time::Duration::ZERO;
        let mut total_mem_read_time = std::time::Duration::ZERO;
        let mut total_match_time = std::time::Duration::ZERO;
        let mut total_insert_time = std::time::Duration::ZERO;

        let mut iteration_count = 0;
        let mut mem_read_count = 0;
        let mut match_count = 0;
        let mut insert_count = 0;

        let overall_start = Instant::now();

        for pair in existing_results.iter() {
            let iter_start = Instant::now();
            let addr = pair.addr;
            let buffer_size = pair.value_type.size();
            total_iteration_time += iter_start.elapsed();
            iteration_count += 1;

            let read_start = Instant::now();
            let read_result = mem.mem_read(addr, buffer_size);
            total_mem_read_time += read_start.elapsed();
            mem_read_count += 1;

            if let Ok(buffer) = read_result {
                let match_start = Instant::now();
                let match_result = search_value.matched(&buffer);
                total_match_time += match_start.elapsed();
                match_count += 1;

                if let Ok(true) = match_result {
                    let insert_start = Instant::now();
                    refined_results.push(pair.clone());
                    total_insert_time += insert_start.elapsed();
                    insert_count += 1;
                }
            }
        }

        let total_time = overall_start.elapsed();

        let stats = BenchmarkStats {
            total_time,
            iteration_time: total_iteration_time,
            mem_read_time: total_mem_read_time,
            match_time: total_match_time,
            insert_time: total_insert_time,
            iteration_count,
            mem_read_count,
            match_count,
            insert_count,
            result_count: refined_results.len(),
        };

        Ok((refined_results, stats))
    }

    /// Statistics for benchmark results
    #[derive(Debug, Clone)]
    struct BenchmarkStats {
        total_time: std::time::Duration,
        iteration_time: std::time::Duration,
        mem_read_time: std::time::Duration,
        match_time: std::time::Duration,
        insert_time: std::time::Duration,
        iteration_count: usize,
        mem_read_count: usize,
        match_count: usize,
        insert_count: usize,
        result_count: usize,
    }

    impl BenchmarkStats {
        fn print_report(&self) {
            println!("\n╔════════════════════════════════════════════════════════╗");
            println!("║           Refine Search Performance Report            ║");
            println!("╠════════════════════════════════════════════════════════╣");

            println!(
                "║ Total Time:          {:>8.2} ms  (100.0%)",
                self.total_time.as_secs_f64() * 1000.0
            );
            println!("╠────────────────────────────────────────────────────────╣");

            let total_ms = self.total_time.as_secs_f64() * 1000.0;
            let iter_ms = self.iteration_time.as_secs_f64() * 1000.0;
            let read_ms = self.mem_read_time.as_secs_f64() * 1000.0;
            let match_ms = self.match_time.as_secs_f64() * 1000.0;
            let insert_ms = self.insert_time.as_secs_f64() * 1000.0;

            println!(
                "║ Iteration:           {:>8.2} ms  ({:>5.1}%)",
                iter_ms,
                (iter_ms / total_ms) * 100.0
            );
            println!(
                "║ Memory Read:         {:>8.2} ms  ({:>5.1}%)",
                read_ms,
                (read_ms / total_ms) * 100.0
            );
            println!(
                "║ Value Matching:      {:>8.2} ms  ({:>5.1}%)",
                match_ms,
                (match_ms / total_ms) * 100.0
            );
            println!(
                "║ Result Insert:       {:>8.2} ms  ({:>5.1}%)",
                insert_ms,
                (insert_ms / total_ms) * 100.0
            );
            println!("╠────────────────────────────────────────────────────────╣");

            println!("║ Operations Count:                                      ║");
            println!("║   Iterations:        {:>8}", self.iteration_count);
            println!("║   Memory Reads:      {:>8}", self.mem_read_count);
            println!("║   Match Checks:      {:>8}", self.match_count);
            println!("║   Inserts:           {:>8}", self.insert_count);
            println!("║   Final Results:     {:>8}", self.result_count);
            println!("╠────────────────────────────────────────────────────────╣");

            if self.iteration_count > 0 {
                println!("║ Average Time per Operation:                            ║");
                println!(
                    "║   Per Iteration:     {:>8.2} µs",
                    (iter_ms * 1000.0) / self.iteration_count as f64
                );
                println!(
                    "║   Per Memory Read:   {:>8.2} µs",
                    (read_ms * 1000.0) / self.mem_read_count as f64
                );
                if self.match_count > 0 {
                    println!(
                        "║   Per Match Check:   {:>8.2} µs",
                        (match_ms * 1000.0) / self.match_count as f64
                    );
                }
                if self.insert_count > 0 {
                    println!(
                        "║   Per Insert:        {:>8.2} µs",
                        (insert_ms * 1000.0) / self.insert_count as f64
                    );
                }
            }

            println!("╚════════════════════════════════════════════════════════╝\n");
        }
    }

    fn refine_results_group_unordered(
        mem: &MockMemory,
        existing_results: &BPlusTreeSet<ValuePair>,
        query: &SearchQuery,
    ) -> Result<Vec<ValuePair>> {
        let mut refined_results = Vec::with_capacity(existing_results.len());

        // Read current values at all existing addresses
        // Note: BPlusTreeSet already returns results sorted by address
        let mut addr_values: Vec<(u64, Vec<u8>)> = Vec::with_capacity(existing_results.len());
        for pair in existing_results.iter() {
            let addr = pair.addr;
            let value_size = pair.value_type.size();

            if let Ok(buffer) = mem.mem_read(addr, value_size) {
                addr_values.push((addr, buffer));
            }
        }

        let mut anchor_address = BTreeSet::new();
        for i in 0..addr_values.len() {
            let (anchor_addr, ref value) = addr_values[i];
            if query.values[0].matched(value)? {
                anchor_address.insert(anchor_addr);
            }
        }

        let mut matched_candidates: HashSet<_> = HashSet::new();
        // For each address as an anchor point
        for i in 0..addr_values.len() {
            let (anchor_addr, _) = addr_values[i];

            // Define search range based on mode
            let (min_addr, max_addr) = (
                anchor_addr.saturating_sub(query.range as u64),
                anchor_addr + query.range as u64,
            );

            // Collect all candidate addresses within range
            let candidates = addr_values
                .iter()
                .enumerate()
                .filter_map(|(_idx, (addr, value))| {
                    if *addr >= min_addr && *addr <= max_addr {
                        Some((*addr, value))
                    } else {
                        None
                    }
                })
                .collect::<Vec<_>>();

            let mut matched_address = Vec::with_capacity(query.values.len());
            let mut used_address = BPlusTreeSet::new(BPLUS_TREE_ORDER);

            for search_value in &query.values {
                let mut found = false;

                // Search from the beginning of candidates (critical for unordered mode!)
                for &(addr, value_bytes) in &candidates {
                    if used_address.contains(&addr) {
                        continue;
                    }

                    if let Ok(true) = search_value.matched(value_bytes) {
                        matched_address.push((addr, search_value.value_type()));
                        used_address.insert(addr);
                        found = true;
                        break;
                    }
                }

                if !found {
                    // This query value couldn't be matched, pattern fails
                    matched_address.clear();
                    used_address.clear();
                    break;
                }
            } // foreach query.values

            if matched_address.len() >= query.values.len() {
                if matched_candidates.contains(&matched_address) {
                    continue;
                }

                matched_candidates.insert(matched_address.clone());

                // All query values matched, add to results
                for (addr, value_type) in matched_address {
                    refined_results.push(ValuePair::new(addr, value_type));
                }
            }
        }

        Ok(refined_results)
    }

    fn refine_results_group_ordered(
        mem: &MockMemory,
        existing_results: &BPlusTreeSet<ValuePair>,
        query: &SearchQuery,
    ) -> Result<Vec<ValuePair>> {
        todo!()
    }

    fn refine_results_group(
        mem: &MockMemory,
        existing_results: &BPlusTreeSet<ValuePair>,
        query: &SearchQuery,
    ) -> Result<Vec<ValuePair>> {
        match query.mode {
            SearchMode::Ordered => refine_results_group_ordered(mem, existing_results, query),
            SearchMode::Unordered => refine_results_group_unordered(mem, existing_results, query),
        }
    }

    #[test]
    fn test_single_to_single_refine() -> Result<()> {
        println!("\n=== Test: Single value search → Single value refine ===\n");

        let mut mem = MockMemory::new();
        let base_addr = mem.malloc(0x7000000000, 1 * 1024 * 1024)?; // 1MB

        // Write test data: value1 at offset, value2 at offset+4
        let test_data = vec![
            (0x1000, 100u32, 200u32), // First: ✓, Refine: ✓
            (0x2000, 100u32, 300u32), // First: ✓, Refine: ✗
            (0x3000, 100u32, 200u32), // First: ✓, Refine: ✓
            (0x4000, 150u32, 200u32), // First: ✗
            (0x5000, 100u32, 200u32), // First: ✓, Refine: ✓
            (0x6000, 100u32, 250u32), // First: ✓, Refine: ✗
        ];

        for (offset, val1, val2) in &test_data {
            mem.mem_write_u32(base_addr + offset, *val1)?;
            mem.mem_write_u32(base_addr + offset + 4, *val2)?;
            println!("Write: 0x{:X} = {}, +4 = {}", base_addr + offset, val1, val2);
        }

        // First search: Find all addresses with value 100
        let query1 = SearchValue::fixed(100, ValueType::Dword);
        let results1 = perform_single_search(&mem, &query1, base_addr, 1 * 1024 * 1024)?;

        results1.iter().for_each(|pair| {
            println!("Found: 0x{:X}", pair.addr);
        });

        println!("\nFirst search results: {} matches for value 100", results1.len());
        assert_eq!(results1.len(), 5, "Should find 5 addresses with value 100");

        // Modify some values in memory (simulating value changes)
        mem.mem_write_u32(base_addr + 0x2000, 200u32)?;
        mem.mem_write_u32(base_addr + 0x6000, 200u32)?;

        // Refine search
        let query2 = SearchValue::fixed(200, ValueType::Dword);
        let results2 = refine_results_single(&mem, &results1, &query2)?;

        println!("\nRefine search results: {} addresses have value 200", results2.len());
        assert_eq!(results2.len(), 2, "Should find 2 addresses that have value 200");

        results2.iter().for_each(|pair| {
            println!("Found: 0x{:X}", pair.addr);
        });

        println!("\nTest completed!");
        Ok(())
    }

    #[test]
    fn test_single_to_group_refine_unordered() -> Result<()> {
        println!("\n=== Test: Single value search → Group search refine ===\n");

        let mut mem = MockMemory::new();
        let base_addr = mem.malloc(0x7100000000, 1 * 1024 * 1024)?;

        // Write test data: single value, then check if followed by a pattern
        let test_patterns = vec![
            (0x1000, 100u32),
            (0x2000, 100u32),
            (0x3000, 100u32),
            (0x4000, 150u32),
            (0x5000, 100u32),
        ];

        for (offset, v1) in &test_patterns {
            mem.mem_write_u32(base_addr + offset, *v1)?;
            println!("Write: 0x{:X} = [{}]", base_addr + offset, v1);
        }
        let query1 = SearchValue::fixed(100, ValueType::Dword);
        let results1 = perform_single_search(&mem, &query1, base_addr, 1 * 1024 * 1024)?;

        println!("\nFirst search results: {} matches for value 100", results1.len());
        assert_eq!(results1.len(), 4, "Should find 4 addresses with value 100");

        results1.iter().for_each(|pair| {
            println!("Found: 0x{:X}", pair.addr);
        });

        let query2 = SearchQuery::new(
            vec![
                SearchValue::fixed(200, ValueType::Dword),
                SearchValue::fixed(300, ValueType::Dword),
            ],
            SearchMode::Unordered,
            128,
        );

        let results2 = refine_results_group(&mem, &results1, &query2)?;

        println!(
            "\nRefine search results: {} addresses have pattern [200, 300] nearby",
            results2.len()
        );

        results2.iter().for_each(|pair| {
            println!("Found: 0x{:X}", pair.addr);
        });

        assert_eq!(results2.len(), 0, "Should find 0 addresses with pattern [200, 300]");

        println!("\nTest completed!");
        Ok(())
    }

    #[test]
    fn test_single_to_group_refine_unordered2() -> Result<()> {
        println!("\n=== Test: Single value search → Group search refine ===\n");

        let mut mem = MockMemory::new();
        let base_addr = mem.malloc(0x7100000000, 1 * 1024 * 1024)?;

        // Write test data: single value, then check if followed by a pattern
        let test_patterns = vec![
            (0x1000, 100u32),
            (0x2000, 100u32),
            (0x3000, 100u32),
            (0x4000, 150u32),
            (0x5000, 100u32),
        ];

        for (offset, v1) in &test_patterns {
            mem.mem_write_u32(base_addr + offset, *v1)?;
            println!("Write: 0x{:X} = [{}]", base_addr + offset, v1);
        }
        let query1 = SearchValue::fixed(100, ValueType::Dword);
        let results1 = perform_single_search(&mem, &query1, base_addr, 1 * 1024 * 1024)?;

        assert_eq!(results1.len(), 4); // 找到4个地址才是正确的

        results1.iter().enumerate().for_each(|(index, pair)| {
            println!("Found: 0x{:X}", pair.addr);
            if index == 0 {
                assert_eq!(pair.addr, base_addr + 0x1000);
                mem.mem_write_u32(pair.addr, 200u32).unwrap();
            }
            if index == 1 {
                assert_eq!(pair.addr, base_addr + 0x2000);
                mem.mem_write_u32(pair.addr, 300u32).unwrap();
            }
            if index == 2 {
                assert_eq!(pair.addr, base_addr + 0x3000);
                mem.mem_write_u32(pair.addr, 200u32).unwrap();
            }
            if index == 3 {
                assert_eq!(pair.addr, base_addr + 0x5000);
                mem.mem_write_u32(pair.addr, 400u32).unwrap();
            }
        });

        // 这个时候内存发生改变了
        // Memory Layout Example:
        //
        // Initial Write:
        // ┌─────────────────┬────────┐
        // │ Address         │ Value  │
        // ├─────────────────┼────────┤
        // │ base + 0x1000   │  100   │
        // │ base + 0x2000   │  100   │
        // │ base + 0x3000   │  100   │
        // │ base + 0x4000   │  150   │ ← Not in results1, unchanged
        // │ base + 0x5000   │  100   │
        // └─────────────────┴────────┘
        //
        // After Modification (results1 processing):
        // ┌─────────────────┬────────┬──────────────────┐
        // │ Address         │ Value  │ Modified By      │
        // ├─────────────────┼────────┼──────────────────┤
        // │ base + 0x1000   │  200   │ results1[0] ✓    │
        // │ base + 0x2000   │  300   │ results1[1] ✓    │
        // │ base + 0x3000   │  200   │ results1[2] ✓    │
        // │ base + 0x4000   │  150   │ (unchanged)      │
        // │ base + 0x5000   │  400   │ results1[3] ✓    │
        // └─────────────────┴────────┴──────────────────┘


        // 但是这里我们搜索范围只有上下128字节，所以是找不到的
        let query2 = SearchQuery::new(
            vec![
                SearchValue::fixed(200, ValueType::Dword),
                SearchValue::fixed(300, ValueType::Dword),
            ],
            SearchMode::Unordered,
            128,
        );

        let results2 = refine_results_group(&mem, &results1, &query2)?;

        println!(
            "\nRefine search results: {} addresses have pattern [200, 300]",
            results2.len()
        );
        assert_eq!(0, results2.len());

        results2.iter().for_each(|pair| {
            println!("Found: 0x{:X}", pair.addr);
        });

        println!("\nTest completed!");
        Ok(())
    }

    #[test]
    fn test_single_to_group_refine_unordered3() -> Result<()> {
        println!("\n=== Test: Single value search → Group search refine ===\n");

        let mut mem = MockMemory::new();
        let base_addr = mem.malloc(0x7100000000, 1 * 1024 * 1024)?;

        // Write test data: single value, then check if followed by a pattern
        let test_patterns = vec![
            (0x0, 100u32),
            (0x4, 100u32),
            (0x8, 100u32),
            (0x1000, 150u32),
            (0x2000, 100u32),
        ];

        for (offset, v1) in &test_patterns {
            mem.mem_write_u32(base_addr + offset, *v1)?;
            println!("Write: 0x{:X} = [{}]", base_addr + offset, v1);
        }
        let query1 = SearchValue::fixed(100, ValueType::Dword);
        let results1 = perform_single_search(&mem, &query1, base_addr, 1 * 1024 * 1024)?;

        assert_eq!(results1.len(), 4); // 找到4个地址才是正确的

        results1.iter().enumerate().for_each(|(index, pair)| {
            println!("Found: 0x{:X}", pair.addr);
            if index == 0 {
                assert_eq!(pair.addr, base_addr + 0x0);
                mem.mem_write_u32(pair.addr, 200u32).unwrap();
            }
            if index == 1 {
                assert_eq!(pair.addr, base_addr + 0x4);
                mem.mem_write_u32(pair.addr, 300u32).unwrap();
            }
            if index == 2 {
                assert_eq!(pair.addr, base_addr + 0x8);
                mem.mem_write_u32(pair.addr, 200u32).unwrap();
            }
            if index == 3 {
                assert_eq!(pair.addr, base_addr + 0x2000);
                mem.mem_write_u32(pair.addr, 400u32).unwrap();
            }
        });

        let query2 = SearchQuery::new(
            vec![
                SearchValue::fixed(200, ValueType::Dword),
                SearchValue::fixed(300, ValueType::Dword),
            ],
            SearchMode::Unordered,
            128,
        );

        let results2 = refine_results_group(&mem, &results1, &query2)?;

        println!(
            "\nRefine search results: {} addresses have pattern [200, 300]",
            results2.len()
        );

        results2.iter().for_each(|pair| {
            println!("Found: 0x{:X}", pair.addr);
        });

        println!("\nTest completed!");
        Ok(())
    }
}
