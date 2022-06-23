#!/bin/bash --login
#$ -l h_rt=732000
#$ -j y
#$ -m a
#$ -o ./logfile/logfile_$JOB_NAME.log
#$ -cwd
#$ -pe mp 8
#$ -l mem_free=6G
#$ -N kelheim-mc

date
hostname

jar="matsim-kelheim-2.x-SNAPSHOT.jar"

arguments="$RUN_ARGS"

command="java -Xmx46G -Xms46G -XX:+AlwaysPreTouch $JAVA_OPTS -jar $jar --config kelheim-v2.x-25pct.config.xml $arguments run"

echo ""
echo "command is $command"

echo ""
module add java/17
java -version

$command
