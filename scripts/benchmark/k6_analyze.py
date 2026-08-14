"""解析 k6 --out json 导出结果，按接口(name tag)统计 P95/P99/失败率。

用法: python k6_analyze.py k6-result.json
"""
import sys
import json
from statistics import mean, median


def main(path):
    durations = {}  # name -> [ms]
    failed = {}     # name -> [0/1]

    with open(path, encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                rec = json.loads(line)
            except ValueError:
                continue
            if rec.get('type') != 'Point':
                continue
            metric = rec.get('metric')
            data = rec.get('data', {})
            name = (data.get('tags') or {}).get('name', 'TOTAL')
            if metric == 'http_req_duration':
                durations.setdefault(name, []).append(data['value'])
            elif metric == 'http_req_failed':
                failed.setdefault(name, []).append(data['value'])

    print(f"{'接口':<16} {'请求数':>7} {'avg':>8} {'med':>8} {'p95':>8} {'p99':>8} {'失败率':>8}")
    for name in sorted(durations.keys()):
        ds = sorted(durations[name])
        n = len(ds)
        avg = mean(ds)
        med = median(ds)
        p95 = ds[int(n * 0.95) - 1] if n else 0
        p99 = ds[int(n * 0.99) - 1] if n else 0
        f = failed.get(name, [])
        fr = sum(f) / len(f) * 100 if f else 0
        print(f"{name:<16} {n:>7} {avg/1000:>7.2f}s {med/1000:>7.2f}s "
              f"{p95/1000:>7.2f}s {p99/1000:>7.2f}s {fr:>7.2f}%")


if __name__ == '__main__':
    main(sys.argv[1])
