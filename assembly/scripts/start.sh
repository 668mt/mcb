jarname="@project.artifactId@-@project.version@.jar"
startCmd="java -Xmx200m -Xms200m -XX:+UseG1GC -Dspring.config.additional-location=application.yaml -jar $jarname"
echo $startCmd && nohup $startCmd >> ./logs/${jarname}.out 2>&1 &
