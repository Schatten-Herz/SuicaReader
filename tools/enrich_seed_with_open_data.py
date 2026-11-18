import csv
import json
from collections import defaultdict
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OPEN_DATA_PATH = ROOT / "tools" / "open_data_jp_stations.json"
ASSETS = ROOT / "app" / "src" / "main" / "assets"
STATION_CODE_PATH = ASSETS / "StationCode.csv"
SEED_PATH = ASSETS / "station_coordinates_seed.json"
OVERRIDES_PATH = ASSETS / "station_coordinates_overrides.json"

# Verified coordinates for newer / renamed stations absent or renamed in older open-data snapshots
VERIFIED_EXTRA_COORDS = {
    "高輪ゲートウェイ": (35.635540, 139.740724),
    "虎ノ門ヒルズ": (35.667307, 139.747742),
    "幕張豊砂": (35.657985, 140.027181),
    "羽沢横浜国大": (35.481162, 139.586036),
    "羽田空港第1・第2ターミナル": (35.549809, 139.785962),
    "羽田空港第1ターミナル": (35.549540, 139.784280),
    "羽田空港第2ターミナル": (35.551290, 139.788520),
    "羽田空港第3ターミナル": (35.544676, 139.768968),
    "羽田空港国際線ターミナル": (35.544676, 139.768968),
    "羽田空港国際線ビル": (35.544676, 139.768968),
    "羽田空港第1ビル": (35.549540, 139.784280),
    "羽田空港第2ビル": (35.551290, 139.788520),
    "大阪難波": (34.666920, 135.500390),
    "神戸三宮": (34.693460, 135.195180),
    "阪急三宮": (34.693460, 135.195180),
    "阪神三宮": (34.693460, 135.195180),
    "箕面萱野": (34.832540, 135.489430),
    "箕面船場阪大前": (34.822610, 135.489820),
    "計算科学センター": (34.654810, 135.221535),
    "東京ディズニーランド": (35.635932, 139.878631),
    "東京ディズニーシー": (35.627440, 139.889487),
    "ベイサイド": (35.631750, 139.878090),
    "リゾートゲートウェイ": (35.635570, 139.880550),
    "白石蔵王": (37.994800, 140.632600),
    "岐阜羽島": (35.315660, 136.685620),
    "本庄早稲田": (36.218830, 139.179410),
    "ガーラ湯沢": (36.950550, 138.799440),
    "七戸十和田": (40.719980, 141.153740),
}

# Alias links from StationCode.csv display name -> open-data canonical name
OPEN_DATA_ALIASES = {
    "明治神宮前": "明治神宮前〈原宿〉",
    "鴬谷": "鶯谷",
    "モノレール浜松町": "浜松町",
    "YRP野比": "ＹＲＰ野比",
    "西鉄福岡(天神)": "西鉄福岡（天神）",
    "浜大津": "びわ湖浜大津",
    "皇子山": "京阪大津京",
    "羽田": "天空橋",
    "船小屋": "筑後船小屋",
    "池袋駅": "池袋",
}

# Company -> expected prefecture codes for homonym disambiguation
COMPANY_PREFECTURES = {
    "北海道旅客鉄道": {"01"},
    "札幌市交通局": {"01"},
    "東日本旅客鉄道": {"02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "19", "20"},
    "東京地下鉄": {"12", "13", "11"},
    "東京都交通局": {"13"},
    "東武鉄道": {"09", "10", "11", "12", "13"},
    "西武鉄道": {"11", "13"},
    "京成電鉄": {"12", "13"},
    "京王電鉄": {"13", "14"},
    "小田急電鉄": {"13", "14"},
    "東急電鉄": {"13", "14"},
    "京浜急行電鉄": {"13", "14"},
    "相模鉄道": {"14"},
    "横浜市交通局": {"14"},
    "横浜高速鉄道": {"14"},
    "東京モノレール": {"13"},
    "東京臨海高速鉄道": {"13"},
    "北総鉄道": {"12", "13"},
    "東海旅客鉄道": {"13", "14", "21", "22", "23", "24", "25", "26", "27"},
    "名古屋市交通局": {"23"},
    "名古屋鉄道": {"21", "23"},
    "愛知環状鉄道": {"23"},
    "西日本旅客鉄道": {"16", "17", "18", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "40"},
    "大阪市高速電気軌道": {"27"},
    "阪急電鉄": {"26", "27", "28"},
    "阪神電気鉄道": {"27", "28"},
    "京阪電気鉄道": {"25", "26", "27"},
    "近畿日本鉄道": {"23", "24", "26", "27", "29"},
    "南海電気鉄道": {"27", "30"},
    "神戸市交通局": {"28"},
    "京都市交通局": {"26"},
    "四国旅客鉄道": {"36", "37", "38", "39"},
    "九州旅客鉄道": {"40", "41", "42", "43", "44", "45", "46"},
    "西日本鉄道": {"40"},
    "福岡市交通局": {"40"},
}


def _average(points):
    lat = sum(p[0] for p in points) / len(points)
    lng = sum(p[1] for p in points) / len(points)
    return lat, lng


def _dominant_cluster_average(points):
    """If a station name exists in multiple distant regions, pick the densest cluster
    (or metro Tokyo/Osaka cluster) rather than averaging into the ocean."""
    clusters = []
    for lat, lng, pref in points:
        placed = False
        for cluster in clusters:
            clat, clng, _ = cluster[0]
            if abs(lat - clat) < 0.20 and abs(lng - clng) < 0.20:
                cluster.append((lat, lng, pref))
                placed = True
                break
        if not placed:
            clusters.append([(lat, lng, pref)])

    # Prefer largest cluster, breaking ties by major metro prefectures (Tokyo/Osaka/Kanagawa/Aichi)
    metro_prefs = {"13", "27", "14", "23", "26", "28", "11", "12", "40"}
    clusters.sort(
        key=lambda c: (
            len(c),
            1 if any(p[2] in metro_prefs for p in c) else 0,
        ),
        reverse=True,
    )
    best = clusters[0]
    return _average([(p[0], p[1]) for p in best])


def _name_variants(name: str):
    variants = {name}
    if "ＪＲ" in name:
        variants.add(name.replace("ＪＲ", "JR"))
    if "JR" in name:
        variants.add(name.replace("JR", "ＪＲ"))
    if "ヶ" in name:
        variants.add(name.replace("ヶ", "ケ"))
    if "ケ" in name:
        variants.add(name.replace("ケ", "ヶ"))
    if "ッ" in name:
        variants.add(name.replace("ッ", "ツ"))
    if "ツ" in name:
        variants.add(name.replace("ツ", "ッ"))
    return variants


def main() -> None:
    if not OPEN_DATA_PATH.exists():
        raise FileNotFoundError(f"Missing open data file: {OPEN_DATA_PATH}")

    open_data = json.loads(OPEN_DATA_PATH.read_text(encoding="utf-8"))
    seed = json.loads(SEED_PATH.read_text(encoding="utf-8")) if SEED_PATH.exists() else {}

    name_to_points = defaultdict(list)

    for group in open_data:
        for st in group.get("stations", []):
            name = (st.get("name_kanji") or "").strip()
            lat = st.get("lat")
            lon = st.get("lon")
            pref = str(st.get("prefecture", "")).strip().zfill(2)
            if not name or lat is None or lon is None:
                continue
            for v in _name_variants(name):
                name_to_points[v].append((float(lat), float(lon), pref))

    for name, points in name_to_points.items():
        lat, lng = _dominant_cluster_average(points)
        seed[name] = {"lat": lat, "lng": lng}

    for name, (lat, lng) in VERIFIED_EXTRA_COORDS.items():
        for v in _name_variants(name):
            seed[v] = {"lat": lat, "lng": lng}

    for alias_name, target_name in OPEN_DATA_ALIASES.items():
        if target_name in seed and alias_name not in seed:
            seed[alias_name] = seed[target_name]

    SEED_PATH.write_text(json.dumps(seed, ensure_ascii=False, indent=2), encoding="utf-8")

    # Build homonym overrides for StationCode.csv stations with multiple distant locations
    overrides = json.loads(OVERRIDES_PATH.read_text(encoding="utf-8")) if OVERRIDES_PATH.exists() else {}
    if STATION_CODE_PATH.exists():
        with STATION_CODE_PATH.open("r", encoding="utf-8") as f:
            reader = csv.reader(f)
            next(reader, None)
            for row in reader:
                if len(row) < 6:
                    continue
                company = row[3].strip()
                station = row[5].strip()
                pts = name_to_points.get(station)
                if not pts or len(pts) < 2:
                    continue
                # Check if points are far apart
                lats = [p[0] for p in pts]
                lngs = [p[1] for p in pts]
                if max(lats) - min(lats) <= 0.25 and max(lngs) - min(lngs) <= 0.25:
                    continue
                expected_prefs = COMPANY_PREFECTURES.get(company)
                if not expected_prefs:
                    continue
                matched = [p for p in pts if p[2] in expected_prefs]
                if not matched:
                    continue
                # Ensure matched points form a single tight area or pick best cluster among matched
                lat, lng = _dominant_cluster_average(matched)
                key = f"{station}|{company}"
                if key not in overrides:
                    overrides[key] = {"lat": round(lat, 5), "lng": round(lng, 5)}

    OVERRIDES_PATH.write_text(json.dumps(overrides, ensure_ascii=False, indent=2), encoding="utf-8")

    print(f"Seed entries total: {len(seed)}")
    print(f"Override entries total: {len(overrides)}")
    print(f"Wrote: {SEED_PATH}")
    print(f"Wrote: {OVERRIDES_PATH}")


if __name__ == "__main__":
    main()
