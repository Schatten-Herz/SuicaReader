import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
STATIONS_PATH = ASSETS / "stations.json"
SEED_PATH = ASSETS / "station_coordinates_seed.json"
OUT_PATH = ASSETS / "station_coordinates.json"
UNRESOLVED_PATH = ASSETS / "station_coordinates_unresolved.txt"


def normalize_station_name(raw_value: str) -> str:
    return raw_value.split(" (", 1)[0].strip()


def main() -> None:
    stations_obj = json.loads(STATIONS_PATH.read_text(encoding="utf-8"))
    seed_obj = json.loads(SEED_PATH.read_text(encoding="utf-8"))

    unique_station_names = sorted({normalize_station_name(v) for v in stations_obj.values() if isinstance(v, str) and v.strip()})

    resolved = {}
    unresolved = []

    for name in unique_station_names:
        coords = seed_obj.get(name)
        if isinstance(coords, dict) and "lat" in coords and "lng" in coords:
            resolved[name] = {
                "lat": float(coords["lat"]),
                "lng": float(coords["lng"]),
            }
        else:
            unresolved.append(name)

    OUT_PATH.write_text(json.dumps(resolved, ensure_ascii=False, indent=2), encoding="utf-8")
    UNRESOLVED_PATH.write_text("\n".join(unresolved), encoding="utf-8")

    print(f"Resolved stations: {len(resolved)}")
    print(f"Unresolved stations: {len(unresolved)}")
    print(f"Wrote: {OUT_PATH}")
    print(f"Wrote: {UNRESOLVED_PATH}")


if __name__ == "__main__":
    main()
