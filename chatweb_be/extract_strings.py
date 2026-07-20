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

    # Regex for string literal, ignoring those in annotations and log statements
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
        r'System\.out'
    ]

    for i, line in enumerate(new_lines):
        if any(re.search(p, line) for p in skip_patterns):
            continue
        
        # find all string literals
        matches = re.finditer(r'"([^"\\]*)"', line)
        for match in matches:
            val = match.group(1)
            # Skip empty, single char, spaces, basic punctuation
            if len(val) < 2 or val.isspace() or val in ['_', '/', '-', '.*']:
                continue
            
            # Generate constant name
            # "error.msg.not_found" -> ERROR_MSG_NOT_FOUND_STRING
            # "reactions." -> REACTIONS_STRING
            # "id" -> ID_STRING
            name = re.sub(r'[^a-zA-Z0-9]+', '_', val).strip('_').upper()
            if not name:
                continue
            
            # Add suffix STRING if not present (except for maybe ones that just look like names)
            if val.startswith('error.') or val.startswith('success.'):
                name += "_STRING"
            elif name == 'ACCESSTOKEN' or name == 'REFRESHTOKEN':
                name = name
            else:
                name += "_STRING"

            # Ensure we don't conflict with existing constants
            # Check if this constant name is already defined somewhere
            is_already_defined = any(name in l for l in lines)
            if not is_already_defined:
                constants[val] = name

    if not constants:
        return

    # Now replace the strings in the file and insert constants
    for i, line in enumerate(new_lines):
        if any(re.search(p, line) for p in skip_patterns):
            continue
        
        # Be careful to only replace string literals, not inside comments (assuming simple format)
        for val, name in constants.items():
            pattern = r'"' + re.escape(val) + r'"'
            new_lines[i] = re.sub(pattern, name, new_lines[i])

    # Insert constants
    constant_lines = []
    for val, name in constants.items():
        constant_lines.append(f'    private static final String {name} = "{val}";\n')
    
    # insert after class start
    # Find the next line after class declaration that is inside the class
    insert_idx = class_start_idx + 1
    # Skip { if it's on a new line
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

# Find all java files
for root, dirs, files in os.walk('src/main/java/com/web/backend'):
    for file in files:
        if file.endswith('.java'):
            process_file(os.path.join(root, file))

