#!/bin/bash

if [ $# -ne 3 ]; then
   echo "Usage: configure_geo_cluster.sh <member_index{1..6}> <primary_node> <secondary_node>"
   exit 1
fi

MEMBER_INDEX=$1
PRIMARY_NODE=$2
SECONDARY_NODE=$3
CONF_DIR=/opt/opendaylight/current/configuration/initial
AKKACONF=${CONF_DIR}/akka.conf
MODULESCONF=${CONF_DIR}/modules.conf
MODULESHARDSCONF=${CONF_DIR}/module-shards.conf
MY_IP=$(hostname -i)
CLUSTER_MASTER=$PRIMARY_NODE
PORT_NUMBER=1

case $MEMBER_INDEX in
[1])
   PORT_NUMBER=4
   ;;
[2])
   PORT_NUMBER=5
   ;;
[3])
   PORT_NUMBER=6
   ;;
[4])
   PORT_NUMBER=4
   CLUSTER_MASTER=$SECONDARY_NODE
   ;;
[5])
   PORT_NUMBER=5
   CLUSTER_MASTER=$SECONDARY_NODE
   ;;
[6])
   PORT_NUMBER=6
   CLUSTER_MASTER=$SECONDARY_NODE
   ;;
*)
   echo "Usage: configure_geo_cluster.sh <primary_node{1..6}> <secondary_node>"
   exit 1
   ;;
esac

cat > $MODULESCONF << 'endModules'
modules = [

        {
                name = "inventory"
                namespace = "urn:opendaylight:inventory"
                shard-strategy = "module"
        },
        {
                name = "topology"
                namespace = "urn:TBD:params:xml:ns:yang:network-topology"
                shard-strategy = "module"
        },
        {
                name = "toaster"
                namespace = "http://netconfcentral.org/ns/toaster"
                shard-strategy = "module"
        }
] 
endModules

cat > $MODULESHARDSCONF << 'moduleShards'
module-shards = [
        {
                name = "default"
                shards = [
                        {
                                name = "default"
                                replicas = ["member-1",
                                "member-2",
                                "member-3",
                                "member-4",
                                "member-5",
                                "member-6"]
                        }
                ]
        },
        {
                name = "inventory"
                shards = [
                        {
                                name="inventory"
                                replicas = ["member-1",
                                "member-2",
                                "member-3",
                                "member-4",
                                "member-5",
                                "member-6"]
                        }
                ]
        },
        {
                name = "topology"
                shards = [
                        {
                                name="topology"
                                replicas = ["member-1",
                                "member-2",
                                "member-3",
                                "member-4",
                                "member-5",
                                "member-6"]
                        }
                ]
        },
        {
                name = "toaster"
                shards = [
                        {
                                name="toaster"
                                replicas = ["member-1",
                                "member-2",
                                "member-3",
                                "member-4",
                                "member-5",
                                "member-6"]
                        }
                ]
        }
]
moduleShards

cat > $AKKACONF << 'akkaFile'

odl-cluster-data {
  akka {
    remote {
      artery {
        enabled = off
        canonical.hostname = CLUSTER_MASTER 
        canonical.port = 3026PORT_NUMBER
      }
      netty.tcp {
        bind-hostname = MY_IP
        bind-port = 2550

        hostname = CLUSTER_MASTER
        port = 3026PORT_NUMBER
      }
      # when under load we might trip a false positive on the failure detector
      # transport-failure-detector {
        # heartbeat-interval = 4 s
        # acceptable-heartbeat-pause = 16s
      # }
    }

    cluster {
      # Remove ".tcp" when using artery.
      seed-nodes = ["akka.tcp://opendaylight-cluster-data@PRIMARY_NODE:30264",
                                "akka.tcp://opendaylight-cluster-data@PRIMARY_NODE:30265",
                                "akka.tcp://opendaylight-cluster-data@PRIMARY_NODE:30266",
                                "akka.tcp://opendaylight-cluster-data@SECONDARY_NODE:30264",
                                "akka.tcp://opendaylight-cluster-data@SECONDARY_NODE:30265",
                                "akka.tcp://opendaylight-cluster-data@SECONDARY_NODE:30266"]

      roles = ["member-MEMBER_INDEX"]

    }

    persistence {
      # By default the snapshots/journal directories live in KARAF_HOME. You can choose to put it somewhere else by
      # modifying the following two properties. The directory location specified may be a relative or absolute path.
      # The relative path is always relative to KARAF_HOME.

      # snapshot-store.local.dir = "target/snapshots"
      # journal.leveldb.dir = "target/journal"

      journal {
        leveldb {
          # Set native = off to use a Java-only implementation of leveldb.
          # Note that the Java-only version is not currently considered by Akka to be production quality.

          # native = off
        }
      }
    }
  }
}
akkaFile
sed -i "s/CLUSTER_MASTER/${CLUSTER_MASTER}/" $AKKACONF
sed -i "s/PORT_NUMBER/${PORT_NUMBER}/" $AKKACONF
sed -i "s/MY_IP/${MY_IP}/" $AKKACONF
sed -i "s/PRIMARY_NODE/${PRIMARY_NODE}/" $AKKACONF
sed -i "s/SECONDARY_NODE/${SECONDARY_NODE}/" $AKKACONF
sed -i "s/MEMBER_INDEX/${MEMBER_INDEX}/" $AKKACONF
cat $AKKACONF
