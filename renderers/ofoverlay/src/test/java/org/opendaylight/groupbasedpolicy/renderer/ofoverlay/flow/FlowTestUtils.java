/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint.EndpointManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.AllowAction;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.ChainAction;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.Classifier;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.L4Classifier;
import org.opendaylight.groupbasedpolicy.resolver.PolicyResolver;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionDefinitionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ActionName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClauseName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ConditionMatcherName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ContractId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.EndpointGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2BridgeDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L2FloodDomainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.L3ContextId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SelectorName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubjectName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.SubnetId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.HasDirection.Direction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.Matcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.action.refs.ActionRefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.classifier.refs.ClassifierRefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.condition.matchers.ConditionMatcherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.has.conditions.Condition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.TenantBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.ContractBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.EndpointGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L2FloodDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.L3ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.SubjectFeatureInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.SubnetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.ClauseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.Subject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.SubjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.ConsumerMatchersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.clause.ProviderMatchersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.Rule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.contract.subject.RuleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ConsumerNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.endpoint.group.ProviderNamedSelectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ActionInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.tenants.tenant.subject.feature.instances.ClassifierInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FlowTestUtils {

    public static NodeId nodeId = new NodeId("openflow:1");
    public static NodeConnectorId nodeConnectorId = new NodeConnectorId(nodeId.getValue() + ":4");
    public static NodeConnectorId tunnelId = new NodeConnectorId(nodeId.getValue() + ":42");
    public static L3ContextId l3c = new L3ContextId("2cf51ee4-e996-467e-a277-2d380334a91d");
    public static L2FloodDomainId fd = new L2FloodDomainId("98e1439e-52d2-46f8-bd69-5136e6088771");
    public static TenantId tid = new TenantId("1118c691-8520-47ad-80b8-4cf5e3fe3302");
    public static EndpointGroupId eg2 = new EndpointGroupId("632e5e11-7988-4eb5-8fe6-6c182d890276");
    public static NodeId remoteNodeId = new NodeId("openflow:2");
    public static NodeConnectorId remoteTunnelId = new NodeConnectorId(remoteNodeId.getValue() + ":101");
    public static L2BridgeDomainId bd = new L2BridgeDomainId("c95182ba-7807-43f8-98f7-6c7c720b7639");
    public static EndpointGroupId eg = new EndpointGroupId("36dec84a-08c7-497b-80b6-a0035af72a12");
    public static ContractId cid1 = new ContractId("a5874893-bcd5-46de-96af-3c8d99bedf9f");
    public static ContractId cid2 = new ContractId("b5874893-bcd5-46de-96af-3c8d99bedf9f");
    public static ContractId cid3 = new ContractId("c5874893-bcd5-46de-96af-3c8d99bedf9f");
    public static ContractId cid4 = new ContractId("d5874893-bcd5-46de-96af-3c8d99bedf9f");
    private SubnetId sub = new SubnetId("4fcf8dfc-53b5-4aef-84d3-6b5586992fcb");
    private SubnetId sub2 = new SubnetId("c285a59f-fcb8-42e6-bf29-87ea522fd626");
    private SubnetId sub3 = new SubnetId("a0380d52-2a25-48ef-882c-a4d4cd9e00ec");

    public OfContext initCtx(PolicyManager policyManager, PolicyResolver policyResolver, SwitchManager switchManager,
                             EndpointManager endpointManager) {
        return new OfContext(null,
                null,
                policyManager,
                policyResolver,
                switchManager,
                endpointManager,
                null);
    }

    protected TenantBuilder baseTenant() {
        return new TenantBuilder()
                .setId(tid)
                .setEndpointGroup(ImmutableList.of(new EndpointGroupBuilder()
                                .setId(eg)
                                .setNetworkDomain(sub)
                                .setProviderNamedSelector(ImmutableList.of(new ProviderNamedSelectorBuilder()
                                        .setName(new SelectorName("pns1"))
                                        .setContract(ImmutableList.of(cid1, cid2))
                                        .build()))
                                .setConsumerNamedSelector(ImmutableList.of(new ConsumerNamedSelectorBuilder()
                                        .setName(new SelectorName("cns1"))
                                        .setContract(ImmutableList.of(cid3, cid4))
                                        .build()))
                                .build(),
                        new EndpointGroupBuilder()
                                .setId(eg2)
                                .setNetworkDomain(sub2)
                                .setProviderNamedSelector(ImmutableList.of(new ProviderNamedSelectorBuilder()
                                        .setName(new SelectorName("pns1"))
                                        .setContract(ImmutableList.of(cid3, cid4))
                                        .build()))
                                .setConsumerNamedSelector(ImmutableList.of(new ConsumerNamedSelectorBuilder()
                                        .setName(new SelectorName("cns1"))
                                        .setContract(ImmutableList.of(cid1, cid2))
                                        .build()))
                                .build()))
                .setL3Context(ImmutableList.of(new L3ContextBuilder()
                        .setId(l3c)
                        .build()))
                .setL2BridgeDomain(ImmutableList.of(new L2BridgeDomainBuilder()
                        .setId(bd)
                        .setParent(l3c)
                        .build()))
                .setL2FloodDomain(ImmutableList.of(new L2FloodDomainBuilder()
                        .setId(fd)
                        .setParent(bd)
                        .build()))
                .setSubnet(ImmutableList.of(new SubnetBuilder()
                                .setId(sub2)
                                .setParent(fd)
                                .setIpPrefix(new IpPrefix(new Ipv4Prefix("10.0.1.0/24")))
                                .setVirtualRouterIp(new IpAddress(new Ipv4Address("10.0.1.1")))
                                .build(),
                        new SubnetBuilder()
                                .setId(sub)
                                .setParent(fd)
                                .setIpPrefix(new IpPrefix(new Ipv4Prefix("10.0.0.0/24")))
                                .setVirtualRouterIp(new IpAddress(new Ipv4Address("10.0.0.1")))
                                .build(),
                        new SubnetBuilder()
                                .setId(sub3)
                                .setParent(bd)
                                .setIpPrefix(new IpPrefix(new Ipv4Prefix("10.0.2.0/24")))
                                .setVirtualRouterIp(new IpAddress(new Ipv4Address("10.0.2.1")))
                                .build()))
                .setSubjectFeatureInstances(new SubjectFeatureInstancesBuilder()
                        .setClassifierInstance(ImmutableList.of(new ClassifierInstanceBuilder()
                                        .setName(new ClassifierName("tcp_dst_80"))
                                        .setClassifierDefinitionId(L4Classifier.DEFINITION.getId())
                                        .setParameterValue(ImmutableList.of(new ParameterValueBuilder()
                                                        .setName(new ParameterName("destport"))
                                                        .setIntValue(80L)
                                                        .build(),
                                                new ParameterValueBuilder()
                                                        .setName(new ParameterName("proto"))
                                                        .setIntValue(6L)
                                                        .build()))
                                        .build(),
                                new ClassifierInstanceBuilder()
                                        .setName(new ClassifierName("tcp_src_80"))
                                        .setClassifierDefinitionId(Classifier.L4_CL.getId())
                                        .setParameterValue(ImmutableList.of(new ParameterValueBuilder()
                                                        .setName(new ParameterName("sourceport"))
                                                        .setIntValue(80L)
                                                        .build(),
                                                new ParameterValueBuilder()
                                                        .setName(new ParameterName("proto"))
                                                        .setIntValue(6L)
                                                        .build()))
                                        .build(),
                                new ClassifierInstanceBuilder()
                                        .setName(new ClassifierName("ether_type"))
                                        .setClassifierDefinitionId(Classifier.ETHER_TYPE_CL.getId())
                                        .setParameterValue(ImmutableList.of(new ParameterValueBuilder()
                                                .setName(new ParameterName("ethertype"))
                                                .setIntValue(FlowUtils.IPv4)
                                                .build()))
                                        .build()))
                        .setActionInstance(ImmutableList.of(new ActionInstanceBuilder()
                                        .setName(new ActionName("Allow"))
                                        .setActionDefinitionId(new AllowAction().getId())
                                        .build(),
                                new ActionInstanceBuilder().setName(new ActionName("chain1"))
                                        .setActionDefinitionId(new ActionDefinitionId(new ChainAction().getId())).build()))
                        .build());
    }

    protected ContractBuilder baseContract(List<Subject> subjects) {
        ContractBuilder contractBuilder = new ContractBuilder().setId(cid1).setSubject(subjects);
        // TODO refactor
        if (subjects == null) {
            return contractBuilder.setClause(ImmutableList.of(new ClauseBuilder().setName(new ClauseName("test"))
                    .setSubjectRefs(ImmutableList.of(new SubjectName("s1")))
                    .build()));
        }
        List<SubjectName> subjectNames = new ArrayList<>();
        for (Subject subject : subjects) {
            subjectNames.add(subject.getName());
        }
        return contractBuilder.setClause(ImmutableList.of(new ClauseBuilder().setName(new ClauseName("test"))
                .setSubjectRefs(subjectNames)
                .build()));
    }

    protected SubjectBuilder baseSubject(String subjectName, Direction direction, String actionName) {
        return new SubjectBuilder()
                .setName(new SubjectName(subjectName))
                .setRule(ImmutableList.of(new RuleBuilder()
                        .setActionRef(ImmutableList.of(new ActionRefBuilder()
                                .setName(new ActionName(actionName))
                                .setOrder(0)
                                .build()))
                        .setClassifierRef(ImmutableList.of(new ClassifierRefBuilder()
                                .setName(new ClassifierName("tcp_dst_80"))
                                .setDirection(direction)
                                .setInstanceName(new ClassifierName("tcp_dst_80"))
                                .build(), new ClassifierRefBuilder()
                                .setName(new ClassifierName("tcp_dst_80"))
                                .setDirection(Direction.Out)
                                .setInstanceName(new ClassifierName("tcp_dst_80"))
                                .build()))

                        .build()));
    }

    protected ClauseBuilder baseClause(String clauseName, String subjectName, Condition cond1, Condition cond2) {
        return new ClauseBuilder().setName(new ClauseName(clauseName))
                .setSubjectRefs(ImmutableList.of(new SubjectName(subjectName)))
                .setConsumerMatchers(
                        new ConsumerMatchersBuilder().setConditionMatcher(
                                ImmutableList.of(new ConditionMatcherBuilder().setName(
                                        new ConditionMatcherName("m1" + subjectName))
                                        .setCondition(ImmutableList.of(cond1, cond2))
                                        .setMatchType(Matcher.MatchType.Any)
                                        .build())).build())
                .setProviderMatchers(
                        new ProviderMatchersBuilder().setConditionMatcher(
                                ImmutableList.of(new ConditionMatcherBuilder().setName(
                                        new ConditionMatcherName("m2" + subjectName))
                                        .setCondition(ImmutableList.of(cond1, cond2))
                                        .setMatchType(Matcher.MatchType.All)
                                        .build())).build());
    }

    protected Subject createSubject(String name, List<Rule> rules) {
        return new SubjectBuilder().setName(new SubjectName(name)).setRule(rules).build();
    }

    protected List<ClassifierRef> createClassifierRefs(Map<String, Direction> refNamesAndDirections) {
        List<ClassifierRef> refs = new ArrayList<>();
        for (String refName : refNamesAndDirections.keySet()) {
            refs.add(new ClassifierRefBuilder().setName(new ClassifierName(refName))
                    .setDirection(refNamesAndDirections.get(refName))
                    .setInstanceName(new ClassifierName(refName))
                    .build());
        }
        return refs;
    }

    protected EndpointBuilder baseEP() {
        return new EndpointBuilder()
                .setL2Context(bd)
                .setTenant(tid)
                .setEndpointGroup(eg)
                .setMacAddress(new MacAddress("00:00:00:00:00:01"));
    }

    public EndpointBuilder localEP() {
        OfOverlayContext ofc = new OfOverlayContextBuilder()
                .setNodeId(nodeId)
                .setNodeConnectorId(nodeConnectorId)
                .build();
        return baseEP()
                .addAugmentation(OfOverlayContext.class, ofc);
    }

    public EndpointBuilder remoteEP(NodeId id) {
        OfOverlayContext ofc = new OfOverlayContextBuilder()
                .setNodeId(id)
                .setNodeConnectorId(new NodeConnectorId(id.getValue() + ":5"))
                .build();
        return baseEP()
                .setMacAddress(new MacAddress("00:00:00:00:00:02"))
                .addAugmentation(OfOverlayContext.class, ofc);
    }
}