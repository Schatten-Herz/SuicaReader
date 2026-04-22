import csv
import json
import os

stations = {}

with open('StationCode.csv', 'r', encoding='utf-8') as f:
    reader = csv.reader(f)
    next(reader) # Skip header
    for row in reader:
        if len(row) < 6:
            continue
        try:
            # hex strings like "1", "A", "FF"
            area = row[0].zfill(2).upper()
            line = row[1].zfill(2).upper()
            sta = row[2].zfill(2).upper()
            company = row[3]
            line_name = row[4]
            sta_name = row[5]

            # In some systems (like simple Pasmo/Suica readers), Region is omitted.
            # So we store both "Area-Line-Station" and "Line-Station".
            # For Line-Station, the first one encountered (usually lowest area, like JR East 0) is kept.
            
            # Format: "StationName"
            # Or "StationName (Company Line)" to be more descriptive.
            display_name = f"{sta_name} ({company})"
            
            key_full = f"{area}-{line}-{sta}"
            key_short = f"{line}-{sta}"
            
            if key_full not in stations:
                stations[key_full] = display_name
                
            if key_short not in stations:
                stations[key_short] = display_name
        except Exception as e:
            pass

os.makedirs('app/src/main/assets', exist_ok=True)
with open('app/src/main/assets/stations.json', 'w', encoding='utf-8') as f:
    json.dump(stations, f, ensure_ascii=False, indent=2)

print(f"Generated stations.json with {len(stations)} entries.")
