/**
 * @file src/assets/icons/skills/index.ts
 * @description 모든 SVG 스킬 아이콘을 import하여 하나의 객체로 export합니다.
 *              Next.js의 SVG import는 { src, width, height } 형태의 StaticImageData 객체를 반환합니다.
 *              운영체제: Windows
 */
import {StaticImageData} from 'next/image';

import Abap from './abap.svg';
import Aion from './aion.svg';
import Android from './android.svg';
import Angular from './angular.svg';
import Ansible from './ansible.svg';
import ApacheCordova from './apache cordova.svg';
import Ar from './ar.svg';
import ArgoCD from './argo cd.svg';
import Avalanche from './avalanche.svg';
import Aws from './aws.svg';
import Azure from './azure.svg';
import Bert from './bert.svg';
import Bigquery from './bigquery.svg';
import Blender from './blender.svg';
import Bootstrap from './bootstrap.svg';
import C from './c.svg';
import Csharp from './c#.svg';
import Cplusplus from './c++.svg';
import Cassandra from './cassandra.svg';
import Catboost from './catboost.svg';
import Chainlink from './chainlink.svg';
import Chainx from './chainx.svg';
import Circleci from './circleci.svg';
import CloudFunctions from './cloud functions.svg';
import Codeigniter from './codeigniter.svg';
import Confluence from './confluence.svg';
import Cosmos from './cosmos.svg';
import Dart from './dart.svg';
import DeepLearning from './deep learning.svg';
import Defi from './defi.svg';
import Defichain from './defichain.svg';
import Django from './django.svg';
import Docker from './docker.svg';
import Elasticsearch from './elasticsearch.svg';
import Elrond from './elrond.svg';
import Eos from './eos.svg';
import Ethereum from './ethereum.svg';
import Express from './express.svg';
import Fastapi from './fastapi.svg';
import Firebase from './firebase.svg';
import Flask from './flask.svg';
import Flutter from './flutter.svg';
import Gatsby from './gatsby.svg';
import Gcp from './gcp.svg';
import Git from './git.svg';
import Github from './github.svg';
import GithubActions from './github actions.svg';
import Gitlab from './gitlab.svg';
import GitlabCi from './gitlab ci.svg';
import Go from './go.svg';
import GoogleCloud from './google cloud.svg';
import Grafana from './grafana.svg';
import Graphql from './graphql.svg';
import Grpc from './grpc.svg';
import Hardhat from './hardhat.svg';
import Haskell from './haskell.svg';
import Helm from './helm.svg';
import Html from './html.svg';
import Hyperledger from './hyperledger.svg';
import Insomnia from './insomnia.svg';
import Ios from './ios.svg';
import Iotex from './ioTeX.svg';
import Irisnet from './iRISnet.svg';
import Java from './java.svg';
import Javascript from './javascript.svg';
import Jenkins from './jenkins.svg';
import Jquery from './jquery.svg';
import Jira from './jira.svg';
import Kotlin from './kotlin.svg';
import Kubernetes from './kubernetes.svg';
import KyberNetwork from './kyber network.svg';
import Laravel from './laravel.svg';
import Lightgbm from './lightGBM.svg';
import MachineLearning from './machine learning.svg';
import Matplotlib from './matplotlib.svg';
import Maya from './maya.svg';
import Mobx from './mobx.svg';
import Mongodb from './mongodb.svg';
import Mqtt from './mqtt.svg';
import Mysql from './mysql.svg';
import Nebulas from './nebulas.svg';
import Neo4j from './neo4j.svg';
import Nestjs from './nestjs.svg';
import Nextjs from './next.js.svg';
import Nft from './nft.svg';
import Nlp from './nlp.svg';
import Nodejs from './node.js.svg';
import Notion from './notion.svg';
import Numpy from './numpy.svg';
import ObjectOriented from './object oriented.svg';
import Opencv from './opencv.svg';
import Oracle from './oracle.svg';
import Pandas from './pandas.svg';
import Photoshop from './photoshop.svg';
import Php from './php.svg';
import Polygon from './polygon.svg';
import Postgresql from './postgresql.svg';
import Postman from './postman.svg';
import Prometheus from './prometheus.svg';
import Pyspark from './pyspark.svg';
import Python from './python.svg';
import Pytorch from './pytorch.svg';
import React from './react.svg';
import ReactNative from './react native.svg';
import Redis from './redis.svg';
import Redux from './redux.svg';
import Rest from './rest.svg';
import RubyOnRails from './ruby on rails.svg';
import Ruby from './ruby.svg';
import Rust from './rust.svg';
import Scala from './scala.svg';
import ScikitLearn from './scikit-learn.svg';
import Seaborn from './seaborn.svg';
import Secretnetwork from './secretnetwork.svg';
import Slack from './slack.svg';
import Soap from './soap.svg';
import Solana from './solana.svg';
import Solidity from './solidity.svg';
import Spark from './spark.svg';
import Spring from './spring.svg';
import SpringBoot from './spring boot.svg';
import SpringCloud from './spring cloud.svg';
import Sql from './sql.svg';
import Sqlite from './sqlite.svg';
import Substance from './substance.svg';
import Supabase from './supabase.svg';
import Swift from './swift.svg';
import Tailwind from './tailwind css.svg';
import Tensorflow from './tensorflow.svg';
import Terra from './terra.svg';
import Terraform from './terraform.svg';
import Thorchain from './thorchain.svg';
import Tomochain from './tomochain.svg';
import Transformers from './transformers.svg';
import Truffle from './truffle.svg';
import Typescript from './typescript.svg';
import Unity from './unity.svg';
import UnrealEngine from './unreal engine.svg';
import Vue from './vue.svg';
import Web3 from './web3.svg';
import Websocket from './websocket.svg';
import Xgboost from './xgboost.svg';

const SKILL_ICONS: { [key: string]: StaticImageData } = {
  abap: Abap,
  aion: Aion,
  android: Android,
  angular: Angular,
  ansible: Ansible,
  'apache cordova': ApacheCordova,
  ar: Ar,
  'argo cd': ArgoCD,
  avalanche: Avalanche,
  aws: Aws,
  azure: Azure,
  bert: Bert,
  bigquery: Bigquery,
  blender: Blender,
  bootstrap: Bootstrap,
  c: C,
  'c#': Csharp,
  'c++': Cplusplus,
  cassandra: Cassandra,
  catboost: Catboost,
  chainlink: Chainlink,
  chainx: Chainx,
  circleci: Circleci,
  'cloud functions': CloudFunctions,
  codeigniter: Codeigniter,
  confluence: Confluence,
  cosmos: Cosmos,
  dart: Dart,
  'deep learning': DeepLearning,
  defi: Defi,
  defichain: Defichain,
  django: Django,
  docker: Docker,
  elasticsearch: Elasticsearch,
  elrond: Elrond,
  eos: Eos,
  ethereum: Ethereum,
  express: Express,
  fastapi: Fastapi,
  firebase: Firebase,
  flask: Flask,
  flutter: Flutter,
  gatsby: Gatsby,
  gcp: Gcp,
  git: Git,
  github: Github,
  'github actions': GithubActions,
  gitlab: Gitlab,
  'gitlab ci': GitlabCi,
  go: Go,
  'google cloud': GoogleCloud,
  grafana: Grafana,
  graphql: Graphql,
  grpc: Grpc,
  hardhat: Hardhat,
  haskell: Haskell,
  helm: Helm,
  'html/css': Html,
  hyperledger: Hyperledger,
  insomnia: Insomnia,
  ios: Ios,
  iotex: Iotex,
  irisnet: Irisnet,
  java: Java,
  javascript: Javascript,
  jenkins: Jenkins,
  jquery: Jquery,
  jira: Jira,
  kotlin: Kotlin,
  kubernetes: Kubernetes,
  'kyber network': KyberNetwork,
  laravel: Laravel,
  lightgbm: Lightgbm,
  'machine learning': MachineLearning,
  matplotlib: Matplotlib,
  maya: Maya,
  mobx: Mobx,
  mongodb: Mongodb,
  mqtt: Mqtt,
  mysql: Mysql,
  nebulas: Nebulas,
  neo4j: Neo4j,
  nestjs: Nestjs,
  'next.js': Nextjs,
  nft: Nft,
  nlp: Nlp,
  'node.js': Nodejs,
  notion: Notion,
  numpy: Numpy,
  'object oriented': ObjectOriented,
  opencv: Opencv,
  oracle: Oracle,
  pandas: Pandas,
  photoshop: Photoshop,
  php: Php,
  polygon: Polygon,
  postgresql: Postgresql,
  postman: Postman,
  prometheus: Prometheus,
  pyspark: Pyspark,
  python: Python,
  pytorch: Pytorch,
  react: React,
  'react native': ReactNative,
  redis: Redis,
  redux: Redux,
  rest: Rest,
  'ruby on rails': RubyOnRails,
  ruby: Ruby,
  rust: Rust,
  scala: Scala,
  'scikit-learn': ScikitLearn,
  seaborn: Seaborn,
  secretnetwork: Secretnetwork,
  slack: Slack,
  soap: Soap,
  solana: Solana,
  solidity: Solidity,
  spark: Spark,
  spring: Spring,
  'spring boot': SpringBoot,
  'spring cloud': SpringCloud,
  sql: Sql,
  sqlite: Sqlite,
  substance: Substance,
  supabase: Supabase,
  swift: Swift,
  tailwind: Tailwind,
  tensorflow: Tensorflow,
  terra: Terra,
  terraform: Terraform,
  thorchain: Thorchain,
  tomochain: Tomochain,
  transformers: Transformers,
  truffle: Truffle,
  typescript: Typescript,
  unity: Unity,
  'unreal engine': UnrealEngine,
  vue: Vue,
  web3: Web3,
  websocket: Websocket,
  xgboost: Xgboost,
};

export default SKILL_ICONS;