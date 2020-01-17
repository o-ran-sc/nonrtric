FROM python:3

WORKDIR /usr/src/app

RUN pip install connexion[swagger-ui]

COPY policy_instance_1_STD_QoSNudging_0.1.0.json policy_instance_1_STD_QoSNudging_0.1.0.json
COPY policy_instance_1_bis_STD_QoSNudging_0.1.0.json policy_instance_1_bis_STD_QoSNudging_0.1.0.json
COPY policy_instance_2_STD_QoSNudging_0.1.0.json policy_instance_2_STD_QoSNudging_0.1.0.json
COPY policy_type_STD_QoSNudging_0.1.0.json policy_type_STD_QoSNudging_0.1.0.json

COPY a1.py a1.py
COPY main.py main.py
COPY var_declaration.py var_declaration.py

COPY commands.sh commands.sh
COPY run_me.sh run_me.sh

COPY a1-openapi.yaml a1-openapi.yaml

CMD ["/bin/bash", "./run_me.sh"]
