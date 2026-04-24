import json
from collections import defaultdict
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OPEN_DATA_PATH = ROOT / "tools" / "open_data_jp_stations.json"
SEED_PATH = ROOT / "app" / "src" / "main" / "assets" / "station_coordinates_seed.json"

# Tokyo(13), Kyoto(26), Osaka(27)
TARGET_PREFECTURES = {"13", "26", "27"}

IMPORTANT_CITY_HUBS = {
    "札幌", "新千歳空港", "函館", "旭川",
    "仙台", "盛岡", "秋田", "青森", "山形", "福島",
    "新潟", "長野", "金沢", "富山", "福井",
    "名古屋", "岐阜", "静岡", "浜松",
    "神戸", "三ノ宮", "姫路", "奈良", "和歌山",
    "岡山", "広島", "米子", "松江", "山口",
    "高松", "松山", "徳島", "高知",
    "博多", "天神", "小倉", "熊本", "長崎", "大分", "宮崎", "鹿児島中央",
    "那覇空港", "県庁前",
}


def _average(points):
    lat = sum(p[0] for p in points) / len(points)
    lng = sum(p[1] for p in points) / len(points)
    return lat, lng


def main() -> None:
    if not OPEN_DATA_PATH.exists():
        raise FileNotFoundError(f"Missing open data file: {OPEN_DATA_PATH}")

    open_data = json.loads(OPEN_DATA_PATH.read_text(encoding="utf-8"))
    seed = json.loads(SEED_PATH.read_text(encoding="utf-8")) if SEED_PATH.exists() else {}

    name_to_points = defaultdict(list)
    name_to_prefs = defaultdict(set)

    for group in open_data:
        stations = group.get("stations", [])
        for st in stations:
            name = (st.get("name_kanji") or "").strip()
            if not name:
                continue
            lat = st.get("lat")
            lon = st.get("lon")
            pref = str(st.get("prefecture", "")).strip()
            if lat is None or lon is None:
                continue
            name_to_points[name].append((float(lat), float(lon)))
            if pref:
                name_to_prefs[name].add(pref)

    added_pref = 0
    added_hub = 0
    updated_existing = 0

    for name, points in name_to_points.items():
        prefs = name_to_prefs[name]
        is_target_pref_station = any(p in TARGET_PREFECTURES for p in prefs)
        is_important_hub = name in IMPORTANT_CITY_HUBS
        if not (is_target_pref_station or is_important_hub):
            continue

        lat, lng = _average(points)
        before = seed.get(name)
        seed[name] = {"lat": lat, "lng": lng}
        if before is None:
            if is_target_pref_station:
                added_pref += 1
            else:
                added_hub += 1
        else:
            updated_existing += 1

    SEED_PATH.write_text(json.dumps(seed, ensure_ascii=False, indent=2), encoding="utf-8")

    print(f"Seed entries total: {len(seed)}")
    print(f"Added target-pref stations: {added_pref}")
    print(f"Added important hubs: {added_hub}")
    print(f"Updated existing entries: {updated_existing}")
    print(f"Wrote: {SEED_PATH}")


if __name__ == "__main__":
    main()
