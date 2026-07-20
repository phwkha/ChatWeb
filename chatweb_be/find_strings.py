import os
import re
from collections import Counter

strings = []

skip_patterns = [
    r'log\.',
    r'@Operation',
    r'@Tag',
    r'@GetMapping',
    r'@PostMapping',
    r'@PutMapping',
    r'@DeleteMapping',
    r'@RequestMapping',
    r'@PreAuthorize',
    r'@Value',
    r'@CookieValue',
    r'System\.out',
    r'@Column',
    r'@Table',
    r'@Slf4j',
    r'@Profile',
    r'@KafkaListener'
]

for root, dirs, files in os.walk('src/main/java/com/web/backend'):
    for file in files:
        if file.endswith('.java'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r') as f:
                lines = f.readlines()
            
            for line in lines:
                if any(re.search(p, line) for p in skip_patterns):
                    continue
                # Skip import, package, static final String
                if line.strip().startswith('import ') or line.strip().startswith('package '):
                    continue
                if 'static final String' in line:
                    continue
                
                # find strings
                matches = re.finditer(r'"([^"\\]+)"', line)
                for match in matches:
                    val = match.group(1)
                    if len(val) >= 2 and not val.isspace():
                        strings.append(val)

c = Counter(strings)
for k, v in c.most_common(50):
    print(f"{v}: {k}")
