/**
 * @file src/assets/icons/skills/index.ts
 * @description 모든 SVG 스킬 아이콘을 import하여 하나의 객체로 export합니다.
 *              Next.js의 SVG import는 { src, width, height } 형태의 StaticImageData 객체를 반환합니다.
 *              운영체제: Windows
 */
import { StaticImageData } from 'next/image';

import Abap from './abap.svg';
import Angular from './angular.svg';
import Ansible from './ansible.svg';
import ArgoCD from './argo cd.svg';
import Aws from './aws.svg';
import Azure from './azure.svg';
import Bert from './bert.svg';
import Bigquery from './bigquery.svg';
import Bootstrap from './bootstrap.svg';
import C from './c.svg';
import Csharp from './c#.svg';
import Cplusplus from './c++.svg';
import Cassandra from './cassandra.svg';
import Catboost from './catboost.svg';
import Circleci from './circleci.svg';
import CloudFunctions from './cloud functions.svg';
import Codeigniter from './codeigniter.svg';
import Confluence from './confluence.svg';
import Dart from './dart.svg';
import DeepLearning from './deep learning.svg';
import Django from './django.svg';
import Docker from './docker.svg';
import Elasticsearch from './elasticsearch.svg';
import Express from './express.svg';
import Fastapi from './fastapi.svg';
import Firebase from './firebase.svg';
import Flask from './flask.svg';
import Gatsby from './gatsby.svg';
import Gcp from './gcp.svg';
import Git from './git.svg';
import Github from './github.svg';
import GithubActions from './github actions.svg';
import Gitlab from './gitlab.svg';
import GitlabCi from './gitlab ci.svg';
import Go from './go.svg';
import Grafana from './grafana.svg';
import Graphql from './graphql.svg';
import Grpc from './grpc.svg';
import Haskell from './haskell.svg';
import Helm from './helm.svg';
import Html from './html.svg';
import Insomnia from './insomnia.svg';
import Java from './java.svg';
import Javascript from './javascript.svg';
import Jenkins from './jenkins.svg';
import Jquery from './jquery.svg';
import Jira from './jira.svg';
import Kotlin from './kotlin.svg';
import Kubernetes from './kubernetes.svg';
import Laravel from './laravel.svg';
import MachineLearning from './machine learning.svg';
import Matplotlib from './matplotlib.svg';
import Mobx from './mobx.svg';
import Mongodb from './mongodb.svg';
import Mqtt from './mqtt.svg';
import Mysql from './mysql.svg';
import Neo4j from './neo4j.svg';
import Nestjs from './nestjs.svg';
import Nextjs from './next.js.svg';
import Nlp from './nlp.svg';
import Nodejs from './node.js.svg';
import Notion from './notion.svg';
import Numpy from './numpy.svg';
import Opencv from './opencv.svg';
import Oracle from './oracle.svg';
import Pandas from './pandas.svg';
import Php from './php.svg';
import Postgresql from './postgresql.svg';
import Postman from './postman.svg';
import Prometheus from './prometheus.svg';
import Python from './python.svg';
import Pytorch from './pytorch.svg';
import React from './react.svg';
import Redis from './redis.svg';
import Redux from './redux.svg';
import RubyOnRails from './ruby on rails.svg';
import Ruby from './ruby.svg';
import Rust from './rust.svg';
import Scala from './scala.svg';
import ScikitLearn from './scikit-learn.svg';
import Seaborn from './seaborn.svg';
import Slack from './slack.svg';
import Spark from './spark.svg';
import Spring from './spring.svg';
import SpringBoot from './spring boot.svg';
import SpringCloud from './spring cloud.svg';
import Sql from './sql.svg';
import Sqlite from './sqlite.svg';
import Supabase from './supabase.svg';
import Swift from './swift.svg';
import Tailwind from './tailwind.svg';
import Tensorflow from './tensorflow.svg';
import Terraform from './terraform.svg';
import Transformers from './transformers.svg';
import Typescript from './typescript.svg';
import Vue from './vue.svg';
import Websocket from './websocket.svg';

const SKILL_ICONS: { [key: string]: StaticImageData } = {
  abap: Abap,
  angular: Angular,
  ansible: Ansible,
  'argo cd': ArgoCD,
  aws: Aws,
  azure: Azure,
  bert: Bert,
  bigquery: Bigquery,
  bootstrap: Bootstrap,
  c: C,
  'c#': Csharp,
  'c++': Cplusplus,
  cassandra: Cassandra,
  catboost: Catboost,
  circleci: Circleci,
  'cloud functions': CloudFunctions,
  codeigniter: Codeigniter,
  confluence: Confluence,
  dart: Dart,
  'deep learning': DeepLearning,
  django: Django,
  docker: Docker,
  elasticsearch: Elasticsearch,
  express: Express,
  fastapi: Fastapi,
  firebase: Firebase,
  flask: Flask,
  gatsby: Gatsby,
  gcp: Gcp,
  git: Git,
  github: Github,
  'github actions': GithubActions,
  gitlab: Gitlab,
  'gitlab ci': GitlabCi,
  go: Go,
  grafana: Grafana,
  graphql: Graphql,
  grpc: Grpc,
  haskell: Haskell,
  helm: Helm,
  html: Html,
  insomnia: Insomnia,
  java: Java,
  javascript: Javascript,
  jenkins: Jenkins,
  jquery: Jquery,
  jira: Jira,
  kotlin: Kotlin,
  kubernetes: Kubernetes,
  laravel: Laravel,
  'machine learning': MachineLearning,
  matplotlib: Matplotlib,
  mobx: Mobx,
  mongodb: Mongodb,
  mqtt: Mqtt,
  mysql: Mysql,
  neo4j: Neo4j,
  nestjs: Nestjs,
  'next.js': Nextjs,
  nlp: Nlp,
  'node.js': Nodejs,
  notion: Notion,
  numpy: Numpy,
  opencv: Opencv,
  oracle: Oracle,
  pandas: Pandas,
  php: Php,
  postgresql: Postgresql,
  postman: Postman,
  prometheus: Prometheus,
  python: Python,
  pytorch: Pytorch,
  react: React,
  redis: Redis,
  redux: Redux,
  'ruby on rails': RubyOnRails,
  ruby: Ruby,
  rust: Rust,
  scala: Scala,
  'scikit-learn': ScikitLearn,
  seaborn: Seaborn,
  slack: Slack,
  spark: Spark,
  spring: Spring,
  'spring boot': SpringBoot,
  'spring cloud': SpringCloud,
  sql: Sql,
  sqlite: Sqlite,
  supabase: Supabase,
  swift: Swift,
  tailwind: Tailwind,
  tensorflow: Tensorflow,
  terraform: Terraform,
  transformers: Transformers,
  typescript: Typescript,
  vue: Vue,
  websocket: Websocket,
};

export default SKILL_ICONS;