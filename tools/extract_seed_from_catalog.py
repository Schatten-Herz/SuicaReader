import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CATALOG_PATH = ROOT / "app" / "src" / "main" / "java" / "com" / "example" / "suicareader" / "ui" / "map" / "TransitMapCatalog.kt"
SEED_PATH = ROOT / "app" / "src" / "main" / "assets" / "station_coordinates_seed.json"


def main() -> None:
    content = CATALOG_PATH.read_text(encoding="utf-8")
    entries = re.findall(r'"([^"]+)"\s+to\s+LatLng\(([-0-9.]+),\s*([-0-9.]+)\)', content)
    seed = {name: {"lat": float(lat), "lng": float(lng)} for name, lat, lng in entries}
    SEED_PATH.write_text(json.dumps(seed, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Wrote seed: {SEED_PATH} ({len(seed)} entries)")


if __name__ == "__main__":
    main()
