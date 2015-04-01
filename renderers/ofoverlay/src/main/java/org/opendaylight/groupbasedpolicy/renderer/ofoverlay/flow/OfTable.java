/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager.Dirty;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.SwitchManager;
import org.opendaylight.groupbasedpolicy.resolver.PolicyInfo;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage the state of a openflow  table by reacting to any events and updating
 * the table state.  This is an abstract class that must be extended for
 * each specific table being managed.
 * @author readams
 */
public abstract class OfTable {
    protected static final Logger LOG =
            LoggerFactory.getLogger(OfTable.class);

    protected final OfContext ctx;

    public OfTable(OfContext ctx) {
        super();
        this.ctx = ctx;
    }


    // *******
    // OfTable
    // *******

    /**
     * Update the relevant flow table for the node
     * @param nodeId the node to update
     * @param dirty the dirty set
     * @throws Exception
     */
    public abstract void update(NodeId nodeId, 
                                PolicyInfo policyInfo,
                                Dirty dirty) throws Exception;
}
