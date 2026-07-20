import os
import re

def process_file(filepath):
    with open(filepath, 'r') as f:
        lines = f.readlines()

    # Determine where the class starts
    class_start_idx = -1
    for i, line in enumerate(lines):
        if re.search(r'public class \w+', line) or re.search(r'public abstract class \w+', line) or re.search(r'@RestController', line):
            if '{' in line or (i+1 < len(lines) and '{' in lines[i+1]):
                class_start_idx = i if '{' in line else i + 1
                break
    
    if class_start_idx == -1:
        return

    # Find all strings to extract
    constants = {} # value -> constant name
    new_lines = lines.copy()

    # Regex for Translator.tolocale("...")
    for i, line in enumerate(new_lines):
        matches = re.finditer(r'Translator\.tolocale\("([^"\\]+)"\)', line)
        for match in matches:
            val = match.group(1)
            name = re.sub(r'[^a-zA-Z0-9]+', '_', val).strip('_').upper() + "_STRING"
            is_already_defined = any(name in l for l in lines)
            if not is_already_defined:
                constants[val] = name

        # specific matching for "reactions."
        matches2 = re.finditer(r'"(reactions\.)"', line)
        for match in matches2:
            val = match.group(1)
            name = "REACTIONS_STRING"
            is_already_defined = any(name in l for l in lines)
            if not is_already_defined:
                constants[val] = name

    if not constants:
        return

    # Replace
    for i, line in enumerate(new_lines):
        for val, name in constants.items():
            pattern1 = r'Translator\.tolocale\("' + re.escape(val) + r'"\)'
            repl1 = f'Translator.tolocale({name})'
            new_lines[i] = re.sub(pattern1, repl1, new_lines[i])

            if val == "reactions.":
                pattern2 = r'"reactions\."'
                repl2 = name
                new_lines[i] = re.sub(pattern2, repl2, new_lines[i])

    # Insert constants
    constant_lines = []
    for val, name in constants.items():
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
