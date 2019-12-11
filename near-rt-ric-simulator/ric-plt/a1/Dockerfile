FROM python:3

WORKDIR /usr/src/app

RUN pip install connexion[swagger-ui]

COPY pi1.json pi1.json
COPY pi1_bis.json pi1_bis.json
COPY pi2.json pi2.json
COPY STD_QoSNudging_0.1.0.json STD_QoSNudging_0.1.0.json

COPY a1.py a1.py
COPY main.py main.py
COPY var_declaration.py var_declaration.py

COPY commands.sh commands.sh
COPY run_me.sh run_me.sh

COPY a1-openapi.yaml a1-openapi.yaml

CMD ["/bin/bash", "./run_me.sh"]
