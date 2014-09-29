package org.cloudifysource.esc.driver.provisioning.azure.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 *
 */
@XmlType(name = "RoleList")
public class RoleList implements Iterable<Role> {

	private List<Role> roles = new ArrayList<Role>();

	@Override
	public Iterator<Role> iterator() {
		return roles.iterator();
	}

	@XmlElement(name = "Role")
	public List<Role> getRoles() {
		return roles;
	}

	public void setRoles(final List<Role> roles) {
		this.roles = roles;
	}

	/**
	 * Get a Role by name
	 * 
	 * @param roleName
	 * @return Role object, null otherwise
	 */
	public Role getRoleByName(String roleName) {

		if (roles != null && !roles.isEmpty()) {
			for (Role r : roles) {
				if (r.getRoleName().equals(roleName)) {
					return r;
				}
			}
		}
		return null;
	}
}
