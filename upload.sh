echo ip pls
read ec2ip
scp -i ~/.ssh/arbitraderEC2.pem build/libs/arbitrader-1.0-SNAPSHOT-all.jar ec2-user@$ec2ip:/home/ec2-user/
