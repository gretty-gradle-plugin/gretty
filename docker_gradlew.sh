#!/usr/bin/env bash
set -e

function build_docker_gradlew_image(){
  docker build -t "docker_gradlew" .        \
      --build-arg JAVA_VERSIONS="$_javas"     \
      --build-arg GRADLE_VERSION="$_gradle"
}

function run_docker(){
  build_docker_gradlew_image "$*"

  local working_dir="-w //project/${_working_dir}"
  
  local project_volume="-v //$(realpath .)://project"
  
  local gradle_home_volume=""
  if [ "$_gradle_home" ]; then
    gradle_home_volume="-v //$(realpath $_gradle_home)://root/.gradle"
  fi
  
  local params="$DOCKER_ARGS $project_volume $working_dir $gradle_home_volume"
    
  
  echo "RUNNING:" docker run --rm -it $params docker_gradlew "$@"
  docker run --rm -it $params docker_gradlew "$@"
}

function run_docker_gradle() {
    run_docker bash -lc "gradle $*"
}


JDK["8"]="8.0.412-amzn"
JDK["11"]="11.0.23-amzn"
JDK["17"]="17.0.11-amzn"
JDK["21"]="21.0.3-amzn"
 
GRADLE["6"]="6.9.4"
GRADLE["7"]="7.6.5"

POSITIONAL_ARGS=()
while [[ $# -gt 0 ]]; do
    case "$1" in
      -j|--java)              export _javas+=",${JDK[$2]:=$2}"  && shift 2 ;;
      -g|--gradle)            export _gradle=${GRADLE[$2]:=$2}  && shift 2 ;;
      -h|--gradle-home)       export _gradle_home=$2            && shift 2 ;;
      -w|--working-dir)       export _working_dir=$2            && shift 2 ;;
      -b|--bash)              export _bash="Yes"                && shift 1 ;;

      *)                      POSITIONAL_ARGS+=("$1")           && shift  ;;
    esac
done
set -- "${POSITIONAL_ARGS[@]}" # restore positional parameters

if [ "$_bash" ]; then
  run_docker bash -l
else
  run_docker_gradle "${@}"
fi
