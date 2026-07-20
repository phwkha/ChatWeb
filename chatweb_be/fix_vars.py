import os
import re

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Find all occurrences of words that start with a digit and end with _STRING
    # e.g. 10_STRING, 0_STRING
    def replacer(match):
        return 'STR_' + match.group(0)

    # regex to match invalid Java identifier starting with digit and containing _STRING
    new_content = re.sub(r'\b([0-9]+[a-zA-Z0-9_]*_STRING)\b', replacer, content)

    if new_content != content:
        with open(filepath, 'w') as f:
            f.write(new_content)
        print(f"Fixed {filepath}")

for root, dirs, files in os.walk('src/main/java/com/web/backend'):
    for file in files:
        if file.endswith('.java'):
            process_file(os.path.join(root, file))
