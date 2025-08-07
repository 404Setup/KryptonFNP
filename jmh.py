#!/usr/bin/env python3
import json
import sys
import os
from collections import defaultdict

def main():
    if len(sys.argv) != 2:
        print("Usage: ./jmh.py <json_file_path>")
        sys.exit(1)

    json_file = sys.argv[1]

    if not os.path.exists(json_file):
        print(f"Error: File {json_file} does not exist")
        sys.exit(1)

    try:
        with open(json_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except json.JSONDecodeError as e:
        print(f"Error: JSON file parsing failed - {e}")
        sys.exit(1)
    except Exception as e:
        print(f"Error: Failed to read file - {e}")
        sys.exit(1)

    benchmark_data = defaultdict(list)

    for item in data:
        benchmark = item.get('benchmark', '')
        if not benchmark:
            continue

        benchmark_name = benchmark.split('.')[-1]

        score = item.get('primaryMetric', {}).get('score')
        if score is None:
            continue

        score_int = int(score)

        params = item.get('params', {})
        batch_size = params.get('batchSize', '')
        compression_threshold = params.get('compressionThreshold', '')
        data_size = params.get('dataSize', '')
        data_type = params.get('dataType', '')

        params_str = f"{batch_size}:{compression_threshold}:{data_size}:{data_type}"

        benchmark_data[benchmark_name].append((params_str, score_int))

    for benchmark_name, results in benchmark_data.items():
        output_file = f"{benchmark_name}.txt"

        try:
            with open(output_file, 'w', encoding='utf-8') as f:
                for params_str, score_int in results:
                    f.write(f"{params_str} {score_int}\n")

            print(f"Generated file: {output_file} (contains {len(results)} records)")

        except Exception as e:
            print(f"Error: Failed to write file {output_file} - {e}")

if __name__ == "__main__":
    main()