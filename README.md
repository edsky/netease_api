# netease_api
netease API for web.

## 简介
网易云音乐web端API  
通过音乐返回音乐文件地址，其他的API类似

## 文档
- [详细的分析文档](./doc/document.md)

## 支持
目前语言用java所写,可以支持的语言有如下几种:

- C++
- python
- php
- C#
- perl

> 有需要的请提交一下

## JAVA库
[okhttp](http://square.github.io/okhttp/)  
> POST 请求网络支持  

[jce](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html)
> AES 加解密支持, RSA用大数库实现
  
[jackson](http://wiki.fasterxml.com/JacksonHome)  
> json解析支持,需要`databind`

## 已知问题
有些情况下会返回错误(rsa),可能是BigInteger转字符串需要补00进行补码,这种情况可以更换标准RSA算法

## 说明
**代码仅供学习参考**
