#all: hello service count
#
#x := 1
#hello:
#	echo "hello world ${x}"
#
#service:
#	@echo "datalink_service"
#
#count:
#	echo "datalink_service"
#
#s1 s2:
#	@echo $@
# 相当于：
# s1:
#	 echo s1
# s2:
#	 echo s2
HFXT_HOME :=  "."
VERSION ?= "2.14.0"

#REGISTRY ?= "192.168.8.210:8080/library"
REGISTRY ?= "registry.cn-hangzhou.aliyuncs.com"
REPOSITORY_PREF ?= "hfxt/hfxt"
SERVICE_REPOSITORY ?= "${REPOSITORY_PREF}"
WEB_REPOSITORY ?= "${REPOSITORY_PREF}-web"

#$ sudo docker login --username=lijuan.zlj@1774306087113395 registry.cn-hangzhou.aliyuncs.com
TAG ?= 2.12.0
COMMIT_ID := $(shell git rev-parse HEAD)

default: build-all-image

build-all-image: build-hfxt build-hfxt-image build-hfxt-web-image

build-hfxt:
	@echo "mvn install -hfxt"
	@${HFXT_HOME}/mvnw -am \
		-Dmaven.javadoc.skip=true \
		-Drat.skip=true \
		-Djacoco.skip=true \
		-DskipTests \
		-Prelease \
		clean install


build-hfxt-image: build-hfxt
	@echo "build hfxt image"
	@docker buildx build \
		-t ${REGISTRY}/${SERVICE_REPOSITORY}:${TAG} \
	    -f ${HFXT_HOME}/docker/Dockerfile \
		${HFXT_HOME}
#	@docker buildx build --platform linux/amd64 --load \
#		-t ${REGISTRY}/${SERVICE_REPOSITORY}:${TAG} \
#	    -f ${HFXT_HOME}/docker/Dockerfile \
#		${HFXT_HOME}

build-hfxt-web-image: build-hfxt
	@echo "build-hfxt-web image"
	@echo "./docker/create_hop_web_container.sh"
	@sh ./docker/create_hop_web_container.sh
#	@docker buildx build --platform linux/amd64 --load \
#		-t ${REGISTRY}/${WEB_REPOSITORY}:${TAG} \
#	    -f ${HFXT_HOME}/docker/Dockerfile.web \
#		${HFXT_HOME}



publish-all-images: publish-hfxt-image publish-hfxt-web-image
#.PHONY: init
#init:
#	@docker buildx create --name hfxt
#	@docker buildx use hfxt

publish-hfxt-image: build-hfxt
	@echo "build and push hfxt image"
	@docker login --username=lijuan.zlj@1774306087113395 registry.cn-hangzhou.aliyuncs.com
	@docker buildx build --push \
		-t registry.cn-hangzhou.aliyuncs.com/hfxt/hfxt:${VERSION} \
		-f ${HFXT_HOME}/docker/Dockerfile \
        .

publish-hfxt-web-image: build-hfxt
	@echo "build and push hfxt-web image"
	@sh docker/hop_web_docker_before.sh
	@echo "====docker login"
	@docker login --username=lijuan.zlj@1774306087113395 registry.cn-hangzhou.aliyuncs.com
	@docker buildx build \
		-t registry.cn-hangzhou.aliyuncs.com/hfxt/hfxt-web:${VERSION} \
		-f docker/Dockerfile.web \
		.	\
		--push \
#	@sh docker/hop_web_docker_after.sh
	@sh docker/hop_web_docker_after.sh
