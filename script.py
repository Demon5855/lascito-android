import xml.etree.ElementTree as ET

with open(r'c:\Perennia Systems\Lascito\app\src\main\res\drawable\ic_perennia_leaf_logo.xml', 'r', encoding='utf-8') as f:
    detailed_xml = f.read()

ET.register_namespace('android', 'http://schemas.android.com/apk/res/android')
tree = ET.fromstring(detailed_xml)

tree.set('{http://schemas.android.com/apk/res/android}width', '108dp')
tree.set('{http://schemas.android.com/apk/res/android}height', '108dp')
# Important: Add tint to allow adaptive icons to be styled by monochromatic themes
# But for now, we just want the gold color, which is already #C9A96E or #000000. Wait! The detailed logo currently has fillColor="#000000".
# Let's change the fillColor to the main app color #C9A96E for the launcher icon so it looks gorgeous!

group = ET.Element('group', {
    '{http://schemas.android.com/apk/res/android}pivotX': '1024',
    '{http://schemas.android.com/apk/res/android}pivotY': '1024',
    '{http://schemas.android.com/apk/res/android}scaleX': '0.55',
    '{http://schemas.android.com/apk/res/android}scaleY': '0.55'
})

for child in list(tree):
    # Change fillColor to our beautiful gold standard #C9A96E !
    if child.get('{http://schemas.android.com/apk/res/android}fillColor') == '#000000':
        child.set('{http://schemas.android.com/apk/res/android}fillColor', '#C9A96E')
    
    tree.remove(child)
    group.append(child)

tree.append(group)

with open(r'c:\Perennia Systems\Lascito\app\src\main\res\drawable\ic_launcher_foreground.xml', 'w', encoding='utf-8') as f:
    f.write('<?xml version="1.0" encoding="utf-8"?>\n')
    f.write(ET.tostring(tree, encoding='unicode'))
