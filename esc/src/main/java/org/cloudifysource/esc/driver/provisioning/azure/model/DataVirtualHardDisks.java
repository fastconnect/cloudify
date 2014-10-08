package org.cloudifysource.esc.driver.provisioning.azure.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "DataVirtualHardDisks")
@XmlType(name = "DataVirtualHardDisks")
public class DataVirtualHardDisks implements Iterable<DataVirtualHardDisk> {

	private List<DataVirtualHardDisk> dataVirtualHardDisks = new ArrayList<DataVirtualHardDisk>();

	@Override
	public Iterator<DataVirtualHardDisk> iterator() {
		return getDataVirtualHardDisks().iterator();
	}

	@XmlElement(name = "DataVirtualHardDisk")
	public List<DataVirtualHardDisk> getDataVirtualHardDisks() {
		return dataVirtualHardDisks;
	}

	public void setDataVirtualHardDisks(List<DataVirtualHardDisk> dataVirtualHardDisks) {
		this.dataVirtualHardDisks = dataVirtualHardDisks;
	}

}
