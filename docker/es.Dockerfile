# Elasticsearch 8.13.0 + IK 中文分词插件
# 构建命令:
#   docker build -f es.Dockerfile -t cyxz-elasticsearch:8.13.0-ik .
FROM docker.elastic.co/elasticsearch/elasticsearch:8.13.0
RUN bin/elasticsearch-plugin install --batch https://release.infinilabs.com/analysis-ik/stable/elasticsearch-analysis-ik-8.13.0.zip
