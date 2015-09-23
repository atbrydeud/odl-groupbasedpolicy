/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.ovsdb;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.InventoryHelper.addOfOverlayExternalPort;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.InventoryHelper.getLongFromDpid;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.NeutronOvsdbIidFactory.ovsdbNodeAugmentationIid;
import static org.opendaylight.groupbasedpolicy.neutron.ovsdb.util.OvsdbHelper.getNodeFromBridgeRef;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.nodes.node.ExternalInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import static org.opendaylight.groupbasedpolicy.util.DataStoreHelper.removeIfExists;
import static org.opendaylight.groupbasedpolicy.util.DataStoreHelper.submitToDs;

public class NodeDataChangeListener implements DataChangeListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NodeDataChangeListener.class);
    private static final String NEUTRON_PROVIDER_MAPPINGS_KEY = "provider_mappings";
    private static final String INVENTORY_PREFIX = "openflow:";
    private final ListenerRegistration<DataChangeListener> registration;
    private static DataBroker dataBroker;

    public NodeDataChangeListener(DataBroker dataBroker) {
        this.dataBroker = checkNotNull(dataBroker);
        registration = dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, ovsdbNodeAugmentationIid(SouthboundConstants.OVSDB_TOPOLOGY_ID), this,
                DataChangeScope.ONE);
        LOG.trace("NodeDataChangeListener started");
    }

    /*
     * When vSwitch is deleted, we loose data in operational DS to determine Iid of
     * corresponding ExternalInterfaces.
     */
    public static final Map<InstanceIdentifier<OvsdbNodeAugmentation>, InstanceIdentifier<ExternalInterfaces>> nodeIdByExtInterface = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        /*
         * TerminationPoint notifications with OVSDB augmentations
         * vSwitch ports. Iterate through the list of new ports.
         */
        for (Entry<InstanceIdentifier<?>, DataObject> entry : change.getCreatedData().entrySet()) {
            if (entry.getValue() instanceof OvsdbNodeAugmentation) {
                OvsdbNodeAugmentation ovsdbNode = (OvsdbNodeAugmentation) entry.getValue();
                InstanceIdentifier<OvsdbNodeAugmentation> key = (InstanceIdentifier<OvsdbNodeAugmentation>) entry.getKey();
                InstanceIdentifier<ExternalInterfaces> extInterfacesIid = processNodeNotification(ovsdbNode);
                if (extInterfacesIid != null) {
                    nodeIdByExtInterface.put(key, extInterfacesIid);
                }
            }
        }

        /*
         * Updates
         */
        for (Entry<InstanceIdentifier<?>, DataObject> entry : change.getUpdatedData().entrySet()) {
            if (entry.getValue() instanceof OvsdbNodeAugmentation) {
                OvsdbNodeAugmentation ovsdbNode = (OvsdbNodeAugmentation) entry.getValue();
                if (Strings.isNullOrEmpty(getProviderMapping(ovsdbNode))) {
                    removeExternalInterfaces((InstanceIdentifier<OvsdbNodeAugmentation>) entry.getKey());
                }
            }
        }

        /*
         * Deletions
         */
        for (InstanceIdentifier<?> iid : change.getRemovedPaths()) {
            if (iid.getTargetType().equals(OvsdbNodeAugmentation.class)) {
                if (nodeIdByExtInterface.get(iid) != null) {
                    removeExternalInterfaces((InstanceIdentifier<OvsdbNodeAugmentation>) iid);
                }
            }
        }
    }

    @Override
    public void close() throws Exception {
        registration.close();
    }

    public static InstanceIdentifier<ExternalInterfaces> processNodeNotification(OvsdbNodeAugmentation ovsdbNode) {
        LOG.trace("Search for provider mapping on node {}", ovsdbNode);
        String providerPortName = getProviderMapping(ovsdbNode);
        if (providerPortName != null) {
            LOG.trace("Found provider mapping, creating Inventory NodeId");
            String nodeConnectorIdString = getInventoryNodeId(ovsdbNode, providerPortName);
            if (nodeConnectorIdString != null) {
                LOG.trace("Adding OfOverlay External port for {}", nodeConnectorIdString);
                String[] elements = nodeConnectorIdString.split(":");
                String nodeIdString = elements[0] + ":" + elements[1];
                NodeConnectorId ncid = getNodeConnectorId(nodeConnectorIdString);
                return addOfOverlayExternalPort(new NodeId(nodeIdString), ncid, dataBroker);
            }
        }
        return null;
    }

    private void removeExternalInterfaces(InstanceIdentifier<OvsdbNodeAugmentation> iidOvsdbNodeAug){
        InstanceIdentifier<ExternalInterfaces> iidExtInterface = nodeIdByExtInterface.get(iidOvsdbNodeAug);
        ReadWriteTransaction wTx = dataBroker.newReadWriteTransaction();
        removeIfExists(LogicalDatastoreType.CONFIGURATION, iidExtInterface, wTx);
        submitToDs(wTx);
        nodeIdByExtInterface.remove(iidOvsdbNodeAug);
    }

    private static NodeConnectorId getNodeConnectorId(String nodeConnectorIdString) {
        return new NodeConnectorId(nodeConnectorIdString);
    }

    public static String getProviderMapping(OvsdbNodeAugmentation ovsdbNode) {
        if (ovsdbNode.getOpenvswitchOtherConfigs() != null) {
            for (OpenvswitchOtherConfigs config : ovsdbNode.getOpenvswitchOtherConfigs()) {
                if (config.getOtherConfigKey() == null || config.getOtherConfigValue() == null) {
                    continue;
                }
                if (config.getOtherConfigKey().equals(NEUTRON_PROVIDER_MAPPINGS_KEY)) {
                    String otherConfig = config.getOtherConfigValue();
                    if (otherConfig != null) {
                        String[] elements = otherConfig.split(":");
                        if (elements.length == 2) {
                            return elements[1];
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get the DPID and OpenFlow port of the bridge that owns the {@link TerminationPoint} in the
     * provider mapping
     *
     * @return the DPID and OpenFlow port of the bridge that owns the {@link TerminationPoint} in
     * the provider mapping
     */
    private static String getInventoryNodeId(OvsdbNodeAugmentation ovsdbNode, String externalPortName) {
        List<ManagedNodeEntry> ovsdbNodes = ovsdbNode.getManagedNodeEntry();
        if (ovsdbNodes == null) {
            LOG.trace("No ManagedNodeEntry was found on {}", ovsdbNode);
            return null;
        }
        for (ManagedNodeEntry managedNode : ovsdbNodes) {
            if (managedNode.getBridgeRef() != null) {
                /*
                 * Get the Node, then see if it has any TerminationPoint
                 * augmentations. If it does, check each TerminationPoint
                 * augmentation to see if it is the matching provider_mapping
                 */
                Node node = getNodeFromBridgeRef(managedNode.getBridgeRef(), dataBroker);
                if (node == null) {
                    LOG.error("Couldn't get Topology Node for {}", managedNode.getBridgeRef());
                    return null;
                }
                OvsdbBridgeAugmentation ovsdbBridge = node.getAugmentation(OvsdbBridgeAugmentation.class);
                if (ovsdbBridge == null) {
                    LOG.trace("OVSDB Node {} does not contain OvsdbBridgeAugmentation. {}", node.getKey(), node);
                    continue;
                }
                for (TerminationPoint tp : node.getTerminationPoint()) {
                    OvsdbTerminationPointAugmentation tpAug = tp.getAugmentation(OvsdbTerminationPointAugmentation.class);
                    if ((tpAug == null) || (tpAug.getName() == null) || (tpAug.getOfport() == null)) {
                        continue;
                    }
                    if (tpAug.getName().equals(externalPortName)) {
                        return buildInventoryNcid(ovsdbBridge, tpAug);
                    }
                }
            }
        }
        return null;
    }

    private static String buildInventoryNcid(OvsdbBridgeAugmentation ovsdbBridge,
            OvsdbTerminationPointAugmentation terminationPoint) {
        Long macLong = getLongFromDpid(ovsdbBridge.getDatapathId().getValue());
        return INVENTORY_PREFIX + String.valueOf(macLong) + ":" + String.valueOf(terminationPoint.getOfport());
    }
}
