/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.kogito.addons.quarkus.k8s.utils;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.kie.kogito.addons.quarkus.k8s.KubeResourceDiscovery;
import org.kie.kogito.addons.quarkus.k8s.parser.KubeURI;

import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This tests also covers the queryServiceByLabelOrSelector method from {@link ServiceUtils}
 * and queryPodByOwnerReference from {@link PodUtils}
 */
@QuarkusTest
@WithKubernetesTestServer
public class DeploymentUtilsTest {

    @KubernetesTestServer
    KubernetesServer mockServer;
    KubeResourceDiscovery kubeResourceDiscovery;
    private final String namespace = "serverless-workflow-greeting-quarkus";

    @Test
    public void testNotFoundDeployment() {
        kubeResourceDiscovery = new KubeResourceDiscovery(mockServer.getClient());
        Deployment deployment = mockServer.getClient().apps().deployments().inNamespace(namespace)
                .load(this.getClass().getClassLoader().getResourceAsStream("deployment/deployment.yaml")).get();
        deployment.getMetadata().setName("test");
        mockServer.getClient().apps().deployments().inNamespace(namespace).create(deployment);
        assertEquals(Optional.empty(),
                kubeResourceDiscovery.query(new KubeURI("kubernetes:apps/v1/deployment/" + namespace + "/invalid")));
    }

    @Test
    public void testDeploymentWithService() {
        kubeResourceDiscovery = new KubeResourceDiscovery(mockServer.getClient());
        KubeURI kubeURI = new KubeURI("kubernetes:apps/v1/deployment/" + namespace + "/example-deployment-with-service");

        Deployment deployment = mockServer.getClient().apps().deployments().inNamespace(namespace)
                .load(this.getClass().getClassLoader().getResourceAsStream("deployment/deployment.yaml")).get();
        mockServer.getClient().apps().deployments().inNamespace(namespace).create(deployment);

        Service service = mockServer.getClient().services().inNamespace(namespace)
                .load(this.getClass().getClassLoader().getResourceAsStream("deployment/deployment-service.yaml")).get();
        mockServer.getClient().services().inNamespace(namespace).create(service);

        Optional<String> url = kubeResourceDiscovery.query(kubeURI);
        assertEquals("http://10.10.10.11:80", url.get());
    }

    @Test
    public void testDeploymentWithServiceWithCustomPortName() {
        kubeResourceDiscovery = new KubeResourceDiscovery(mockServer.getClient());
        KubeURI kubeURI = new KubeURI("kubernetes:apps/v1/deployment/" + namespace + "/custom-port-deployment?port-name=my-custom-port");

        Deployment deployment = mockServer.getClient().apps().deployments().inNamespace(namespace)
                .load(this.getClass().getClassLoader().getResourceAsStream("deployment/deployment.yaml")).get();
        deployment.getMetadata().setName("custom-port-deployment");
        deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getPorts()
                .add(new ContainerPortBuilder().withName("test-port").withContainerPort(4000).build());
        mockServer.getClient().apps().deployments().inNamespace(namespace).create(deployment);

        Service service = mockServer.getClient().services().inNamespace(namespace)
                .load(this.getClass().getClassLoader().getResourceAsStream("deployment/deployment-service.yaml")).get();
        service.getMetadata().setName("custom-port-name-service");
        service.getSpec().getPorts().add(new ServicePortBuilder()
                .withName("my-custom-port")
                .withTargetPort(new IntOrString("test-port"))
                .withPort(4009).build());
        mockServer.getClient().services().inNamespace(namespace).create(service);

        Optional<String> url = kubeResourceDiscovery.query(kubeURI);
        assertEquals("http://10.10.10.11:4009", url.get());
    }

    @Test
    public void testDeploymentNoService() {
        kubeResourceDiscovery = new KubeResourceDiscovery(mockServer.getClient());
        KubeURI kubeURI = new KubeURI("kubernetes:apps/v1/deployment/" + namespace + "/example-deployment-no-service");

        Deployment deployment = mockServer.getClient().apps().deployments().inNamespace(namespace)
                .load(this.getClass().getClassLoader().getResourceAsStream("deployment/deployment-no-service.yaml")).get();
        Deployment createdDeployment = mockServer.getClient().apps().deployments().inNamespace(namespace).create(deployment);

        ReplicaSet rs = mockServer.getClient().apps().replicaSets().inNamespace(namespace)
                .load(this.getClass().getClassLoader().getResourceAsStream("deployment/replica-set-deployment-no-service.yaml")).get();
        rs.getMetadata().getOwnerReferences().get(0).setUid(createdDeployment.getMetadata().getUid());
        ReplicaSet createdRs = mockServer.getClient().apps().replicaSets().inNamespace(namespace).create(rs);

        Pod pod = mockServer.getClient().pods().inNamespace(namespace)
                .load(this.getClass().getClassLoader().getResourceAsStream("deployment/pod-deployment-no-service.yaml")).get();
        pod.getMetadata().setName("pod-deployment-no-service");
        pod.getMetadata().getOwnerReferences().get(0).setUid(createdRs.getMetadata().getUid());
        mockServer.getClient().pods().inNamespace(namespace).create(pod);

        Optional<String> url = kubeResourceDiscovery.query(kubeURI);
        assertEquals("http://172.17.0.11:8080", url.get());
    }

    @Test
    public void testDeploymentNoService2Replicas() {
        kubeResourceDiscovery = new KubeResourceDiscovery(mockServer.getClient());
        KubeURI kubeURI = new KubeURI("kubernetes:apps/v1/deployment/" + namespace + "/example-deployment-no-service-2-replicas");

        Deployment deployment = mockServer.getClient().apps().deployments().inNamespace(namespace)
                .load(this.getClass().getClassLoader().getResourceAsStream("deployment/deployment-no-service.yaml")).get();
        deployment.getMetadata().setName("example-deployment-no-service-2-replicas");
        deployment.getStatus().setReplicas(2);
        Deployment createdDeployment = mockServer.getClient().apps().deployments().inNamespace(namespace).create(deployment);

        ReplicaSet rs = mockServer.getClient().apps().replicaSets().inNamespace(namespace)
                .load(this.getClass().getClassLoader().getResourceAsStream("deployment/replica-set-deployment-no-service.yaml")).get();
        rs.getMetadata().setName("rs-2-replicas");
        rs.getMetadata().getOwnerReferences().get(0).setUid(createdDeployment.getMetadata().getUid());
        ReplicaSet createdRs = mockServer.getClient().apps().replicaSets().inNamespace(namespace).create(rs);

        Pod pod = mockServer.getClient().pods().inNamespace(namespace)
                .load(this.getClass().getClassLoader().getResourceAsStream("deployment/pod-deployment-no-service.yaml")).get();
        pod.getMetadata().setName("pod-2-replicas");
        pod.getMetadata().getOwnerReferences().get(0).setUid(createdRs.getMetadata().getUid());
        mockServer.getClient().pods().inNamespace(namespace).create(pod);

        Optional<String> url = kubeResourceDiscovery.query(kubeURI);
        assertTrue(url.isEmpty());
    }

    @Test
    public void testDeploymentNoServiceCustomPortName() {
        kubeResourceDiscovery = new KubeResourceDiscovery(mockServer.getClient());
        KubeURI kubeURI = new KubeURI("kubernetes:apps/v1/deployment/" + namespace + "/custom-port-deployment-1?port-name=my-custom-port");

        Deployment deployment = mockServer.getClient().apps().deployments().inNamespace(namespace)
                .load(this.getClass().getClassLoader().getResourceAsStream("deployment/deployment-no-service.yaml")).get();
        deployment.getMetadata().setName("custom-port-deployment-1");
        deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getPorts()
                .add(new ContainerPortBuilder().withName("test-port").withContainerPort(4000).build());
        Deployment createdDeployment = mockServer.getClient().apps().deployments().inNamespace(namespace).create(deployment);

        ReplicaSet rs = mockServer.getClient().apps().replicaSets().inNamespace(namespace)
                .load(this.getClass().getClassLoader().getResourceAsStream("deployment/replica-set-deployment-no-service.yaml")).get();
        rs.getMetadata().setName("custom-port-rs");
        rs.getMetadata().getOwnerReferences().get(0).setUid(createdDeployment.getMetadata().getUid());
        ReplicaSet createdRs = mockServer.getClient().apps().replicaSets().inNamespace(namespace).create(rs);

        Pod pod = mockServer.getClient().pods().inNamespace(namespace)
                .load(this.getClass().getClassLoader().getResourceAsStream("deployment/pod-deployment-no-service.yaml")).get();
        pod.getMetadata().getOwnerReferences().get(0).setUid(createdRs.getMetadata().getUid());
        pod.getSpec().getContainers().get(0).getPorts()
                .add(new ContainerPortBuilder()
                        .withName("my-custom-port")
                        .withContainerPort(4009).build());
        Pod createdPod = mockServer.getClient().pods().inNamespace(namespace).create(pod);

        Optional<String> url = kubeResourceDiscovery.query(kubeURI);
        assertEquals("http://172.17.0.11:4009", url.get());
    }
}
