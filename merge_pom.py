import xml.etree.ElementTree as ET

ET.register_namespace('', "http://maven.apache.org/POM/4.0.0")
ET.register_namespace('xsi', "http://www.w3.org/2001/XMLSchema-instance")

outer_tree = ET.parse('pom.xml')
outer_root = outer_tree.getroot()
ns = {'m': 'http://maven.apache.org/POM/4.0.0'}

inner_tree = ET.parse('main/pom.xml')
inner_root = inner_tree.getroot()

# Remove parent from inner
parent = inner_root.find('m:parent', ns)
if parent is not None:
    inner_root.remove(parent)

# Insert groupId and version to inner POM
version_elem = ET.Element('{http://maven.apache.org/POM/4.0.0}version')
version_elem.text = '1.0'
inner_root.insert(1, version_elem)

group_elem = ET.Element('{http://maven.apache.org/POM/4.0.0}groupId')
group_elem.text = 'com.nym.shortlink'
inner_root.insert(1, group_elem)

# Copy properties, dependencyManagement from outer
properties = outer_root.find('m:properties', ns)
if properties is not None:
    inner_root.insert(4, properties)

dep_mgmt = outer_root.find('m:dependencyManagement', ns)
if dep_mgmt is not None:
    inner_root.insert(5, dep_mgmt)

inner_tree.write('main/pom.xml', encoding='UTF-8', xml_declaration=True)
