/**
 * 
 */
package org.cloudifysource.esc.driver.provisioning.azure.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @author elip
 *
 */

@XmlType(name = "RoleInstanceList")
public class RoleInstanceList implements Iterable<RoleInstance> {

	private List<RoleInstance> roleInstances = new ArrayList<RoleInstance>();

	@Override
	public Iterator<RoleInstance> iterator() {
		return roleInstances.iterator();
	}

	@XmlElement(name = "RoleInstance")
	public List<RoleInstance> getRoleInstances() {
		return roleInstances;
	}

	/**
	 * Get a RoleInstance by its roleName
	 * 
	 * @param roleName
	 * @return Role object, null otherwise
	 */
	public RoleInstance getInstanceRoleByRoleName(String roleName) {

		if (roleInstances != null && !roleInstances.isEmpty()) {
			for (RoleInstance ri : roleInstances) {
				if (ri.getRoleName().equals(roleName)) {
					return ri;
				}
			}
		}
		return null;
	}

}
