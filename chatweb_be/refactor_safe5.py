import os
import re
import string

def is_valid_string(val):
    if len(val) < 2: return False
    if val.isspace(): return False
    if all(c in string.punctuation or c.isspace() for c in val): return False
    # Exclude strings that look like spring expressions or lua scripts
    if val.startswith("return redis.call"): return False
    if "{" in val and "}" in val: return False # Ignore things like {valid.email} as I already handled it
    return True

def process_file(filepath):
    with open(filepath, 'r') as f:
        lines = f.readlines()

    class_start_idx = -1
    for i, line in enumerate(lines):
        if re.search(r'public class \w+', line) or re.search(r'public abstract class \w+', line) or re.search(r'public interface \w+', line) or re.search(r'class \w+', line) or re.search(r'@RestController', line) or re.search(r'@Service', line) or re.search(r'@Component', line):
            if '{' in line or (i+1 < len(lines) and '{' in lines[i+1]):
                class_start_idx = i if '{' in line else i + 1
                break
    
    if class_start_idx == -1:
        return

    constants = {}
    new_lines = lines.copy()

    for i, line in enumerate(new_lines):
        line_stripped = line.strip()
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
                name = re.sub(r'[^a-zA-Z0-9]+', '_', val).strip('_').upper()
                if not name: continue
                if name[0].isdigit():
                    name = "STR_" + name
                name += "_STRING"
                
                is_already_defined = any(name in l for l in lines)
                if not is_already_defined:
                    constants[val] = name

    if not constants:
        return

    # Replace
    for i, line in enumerate(new_lines):
        line_stripped = line.strip()
        if line_stripped.startswith('@'): continue
        if line_stripped.startswith('import '): continue
        if line_stripped.startswith('package '): continue
        if 'static final String' in line: continue
        if 'log.' in line or 'System.out' in line: continue
        if 'LoggerFactory.getLogger' in line: continue

        for val, name in constants.items():
            pattern = r'"' + re.escape(val) + r'"'
            repl = name
            new_lines[i] = re.sub(pattern, repl, new_lines[i])

    constant_lines = []
    for val, name in sorted(constants.items(), key=lambda item: item[1]):
        constant_lines.append(f'    private static final String {name} = "{val}";\n')
    
    insert_idx = class_start_idx + 1
    if '{' in new_lines[class_start_idx]:
        pass
    else:
        for i in range(class_start_idx, len(new_lines)):
            if '{' in new_lines[i]:
                insert_idx = i + 1
                break
    
    constant_lines.append('\n')
    new_lines = new_lines[:insert_idx] + constant_lines + new_lines[insert_idx:]

    with open(filepath, 'w') as f:
        f.writelines(new_lines)
    print(f"Processed {filepath}")

for root, dirs, files in os.walk('src/main/java/com/web/backend'):
    for file in files:
        if file.endswith('.java'):
            process_file(os.path.join(root, file))
