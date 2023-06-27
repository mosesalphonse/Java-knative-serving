# Java-knative-serving
Quarkus Knative Serving - Serverless workloads with JVM and Native Executables

## POC:
* Knative Serving Installation on GKE
* Deploy Sample Java (Quarkus) application run on JVM mode
* Deploy Sample Java (Quarkus) application run on Native Executable mode
* Enable Serverless microservices using Knative (Verify the cold start and compare both JVM and Native Executable workloads)

## Prerequisite

* Kubernetes Cluster
* Docker

# Steps:

(Refer https://knative.dev/docs/install/ for more details)

## Verifying image signatures

From Knative releases 1.9 onwards are signed with cosign.

**Install cosign**__

```
curl -O -L "https://github.com/sigstore/cosign/releases/latest/download/cosign-linux-amd64"
sudo mv cosign-linux-amd64 /usr/local/bin/cosign
sudo chmod +x /usr/local/bin/cosign
```

**Verify KNative signatures**

```
curl -sSL https://github.com/knative/serving/releases/download/knative-v1.10.1/serving-core.yaml \
  | grep 'gcr.io/' | awk '{print $2}' | sort | uniq \
  | xargs -n 1 \
    cosign verify -o text \
      --certificate-identity=signer@knative-releases.iam.gserviceaccount.com \
      --certificate-oidc-issuer=https://accounts.google.com
```

## Install Knative Serving

```
kubectl apply -f https://github.com/knative/serving/releases/download/knative-v1.10.2/serving-crds.yaml

kubectl apply -f https://github.com/knative/serving/releases/download/knative-v1.10.2/serving-core.yaml

```

**Install Network layer:**

There are differnt ways we can route traffic from outside the cluster, for example Istio, Contour, ets. I have used **Kourier** in this demo. 

**Using Kourier**

```
kubectl apply -f https://github.com/knative/net-kourier/releases/download/knative-v1.10.0/kourier.yaml

kubectl patch configmap/config-network \
  --namespace knative-serving \
  --type merge \
  --patch '{"data":{"ingress-class":"kourier.ingress.networking.knative.dev"}}'

```

**Verify Public IP/CNAME**

```
kubectl --namespace kourier-system get service kourier

```

## Verify The installation

```
kubectl get pods -n knative-serving

```

## Configure DNS

There are different ways to configure DNS for the Knative Services, I have used **Magic DNS (sslip.io)**

```
kubectl apply -f https://github.com/knative/serving/releases/download/knative-v1.10.2/serving-default-domain.yaml

```

## KNative Serving Extentions:

**HPA auto scalling**

Knative supports the use of the Kubernetes Horizontal Pod Autoscaler (HPA) for driving autoscaling decisions

```
kubectl apply -f https://github.com/knative/serving/releases/download/knative-v1.10.2/serving-hpa.yaml

```

### Build and Deploy Workloads

**JVM Mode:**
```
git clone https://github.com/mosesalphonse/Java-knative-serving.git

cd Java-knative-serving/workloads

```
Update Application.properties file to make sure the correct image name, version and image repo are updated. 
(Java-knative-serving/workloads/src/main/resources/application.properties)

Refer the below **example**


quarkus.container-image.name={customer-jvm}
quarkus.container-image.tag={v1}
quarkus.container-image.registry={gcr.io}
quarkus.container-image.group={sash-383710}

```
mvn clean package -Dquarkus.container-image.push=true

```
**Note** : Make sure the image should be in the correct image repo. This image repo should be updated in the .yaml manifest (example : image: gcr.io/sash-383710/customer-jvm:v1)
```
cd ..

kubectl apply -f yamls/jvm.yaml

```
**Native Executable:**

Makesure image name updated in the application.properties as below;

quarkus.container-image.name={customer-native}

```
cd workloads

mvn package -Pnative -Dquarkus.native.container-build=true -Dquarkus.container-image.push=true

```
**Note** : Make sure the image should be in the correct image repo. This image repo should be updated in the .yaml manifest (example : gcr.io/sash-383710/customer-native:v1)
```

cd ..

kubectl apply -f yamls/native.yaml

```

### Verify:

a) If no requests for 30 seconds(configurable), automatically pod will drain automatically. Which means there is no compute power used for that pod. If the app is no logger in use, there is no server running behind it. It is one of the serverless characteristics.

**Example** :

![image](https://github.com/mosesalphonse/Java-knative-serving/assets/16347988/0a224c3d-aa23-4aa7-8cc2-858825d278f5)

If any request are made after workloads wre down, it will spin up the envioemnet. Please note thet the JVM mode will take bit longer while Native executable mode are quiker because of AOT(Ahead Of Time) compiler. Refer thebelow screenshot

![image](https://github.com/mosesalphonse/Java-knative-serving/assets/16347988/b89877c5-6ae5-48d5-bb9a-99d27a76fd5f)


Knative Service Endpoints:

```
kubectl get ksvc

```

API resources can be accessed as below;

```
http://customer-jvm.default.{}.sslip.io/customer/info/Moses

http://customer-native.default.{}.sslip.io/customer/info/Sashvin
```

Check the Cold start time using the below;

```
curl -o /dev/null -s -w 'Total: %{time_total}s\n'  http://customer-jvm.default.{}.sslip.io/customer/info/Moses

curl -o /dev/null -s -w 'Total: %{time_total}s\n'  http://customer-native.default.{}.sslip.io/customer/info/Sashvin

```

