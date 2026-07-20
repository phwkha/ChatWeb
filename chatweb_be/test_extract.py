import os
import re
import string

def is_valid_string(val):
    if len(val) < 2: return False
    if val.isspace(): return False
    # Check if string is only punctuation
    if all(c in string.punctuation or c.isspace() for c in val): return False
    return True

extracted = []

for root, dirs, files in os.walk('src/main/java/com/web/backend'):
    for file in files:
        if file.endswith('.java'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r') as f:
                lines = f.readlines()
            
            for line in lines:
                line_stripped = line.strip()
                # Skip annotations, imports, package, logs, constants
                if line_stripped.startswith('@'): continue
                if line_stripped.startswith('import '): continue
                if line_stripped.startswith('package '): continue
                if 'static final String' in line: continue
                if 'log.' in line or 'System.out' in line: continue
                if 'LoggerFactory.getLogger' in line: continue

                matches = re.finditer(r'"([^"\\]+)"', line)
                for match in matches:
                    val = match.group(1)
                    if is_valid_string(val):
                        extracted.append(val)

from collections import Counter
c = Counter(extracted)
print("Top 50 strings to extract:")
for k, v in c.most_common(50):
    print(f"{v}: {k}")
