## 快速分析流程
- 打开音乐列表播放地址 http://music.163.com/playlist?id=536804459
- 切换到单曲 http://music.163.com/#/song?id=37202050
- F12进入开发者模式,Ctrl+P 搜索文件 core.js
- 点击美化源码,Ctrl+F 搜索 `window.asrsea` 形如  `var bua = window.asrsea(JSON.stringify(bl), bbZ(["流泪", "强"]), bbZ(cnb.md), bbZ(["爱心", "女孩", "惊恐", "大笑"]));`
	> `RSAKeyPair` 形如 `d = new RSAKeyPair(b,"",c),`  
	> `CryptoJS.AES.encrypt`  
- 主要加密函数就是**`asrsea`**,下断点 
- console 输出 `bZ` -> API 地址
	> `"/weapi/song/enhance/player/url"`
- 进入函数下断，得到解密后的参数 
	> `function d(d, e, f, g)`  
	> `d` -> 提交参数  `"{"id":"442315372","c":"[{\"id\":\"442315372\"}]","csrf_token":""}"`  
	> `e` -> 模数 010001  
	> `f` -> 公钥 00e0b..  
	> `g` -> AES密钥 "0CoJUm6Qyw8W8jud"

## 详细算法分析
- asrsea
- 随机函数
- AES算法
- RSA算法及详解

#### asrsea函数过程
```
function d(d, e, f, g) {
    var h = {}
      , i = a(16);
    return h.encText = b(d, g),
    h.encText = b(h.encText, i),
    h.encSecKey = c(i, e, f),
    h
}
```

- `i` -> 随机16位key  
- `h.encText = b(d,g)` -> 第一次AES 加密  
- `h.encText = b(h.encText, i)` -> 第二次AES加密  
- `h.encSecKey = c(i,e,f)` -> RSA加密key  

#### 随机函数
```
function a(a) {
    var d, e, b = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789", c = "";
    for (d = 0; a > d; d += 1)
        e = Math.random() * b.length,
        e = Math.floor(e),
        c += b.charAt(e);
    return c
}
```

随机产生16位字符串,字符串 a-zA-Z0-9  


#### AES算法
```
function b(a, b) {
    var c = CryptoJS.enc.Utf8.parse(b)
      , d = CryptoJS.enc.Utf8.parse("0102030405060708")
      , e = CryptoJS.enc.Utf8.parse(a)
      , f = CryptoJS.AES.encrypt(e, c, {
        iv: d,
        mode: CryptoJS.mode.CBC
    });
    return f.toString()
}
```
基本过程如下(a-待加密，b-密钥):  
`base64(aes_cbc(a,b,iv = "0102030405060708"))`

#### RSA算法
```
function c(a, b, c) {
    var d, e;
    return setMaxDigits(131),
    d = new RSAKeyPair(b,"",c),
    e = encryptedString(d, a)
}
```
`setMaxDigits(131)` -> 设置位数1024位,多一位为符号位  
`d = new RSAKeyPair(b,"",c)` -> 生成RSAKey对  
`e = encryptedString(d, a)` -> 使用RSAKey加密

#### encryptedString算法
```
function encryptedString(a, b) {
    for (var f, g, h, i, j, k, l, c = new Array, d = b.length, e = 0; d > e; )
        c[e] = b.charCodeAt(e),
        e++;
    for (; 0 != c.length % a.chunkSize; )
        c[e++] = 0;
    for (f = c.length,
    g = "",
    e = 0; f > e; e += a.chunkSize) {
        for (j = new BigInt,
        h = 0,
        i = e; i < e + a.chunkSize; ++h)
            j.digits[h] = c[i++],
            j.digits[h] += c[i++] << 8;
        k = a.barrett.powMod(j, a.e),
        l = 16 == a.radix ? biToHex(k) : biToString(k, a.radix),
        g += l + " "
    }
    return g.substring(0, g.length - 1)
}
```

其中 a-> RSAKey, b-> 待加密
`for ` -> 第一个for循环 将b的hex流赋值到 数组c中 从低位到高位排序
`for ` -> 第二个for循环 将c剩余的补0
`for ` -> 第三个for循环 按照block方式一块一块加密  
> 这里长度低于128(a.chunkSize==126)字节(1024位)

`for in for`:  -> 将c赋值给大数j  
-> `j.digits[h] = c[i++],` -> 第一个字节放于低位
-> `j.digits[h] += c[i++] << 8;` -> 第二字节放于高位
> `Java BigInteger`构造函数是按照数字书写顺序来的,比如 "123456789987654321" 这个数解析结果就是这个数  
> `JS BigInt`生成字符串也是这种自然顺序,同时在内存里是低位放低位,高位放高位,那么在这里就出现了问题,随机的字符串第一个字节是放于低位的，也就是正常书写的最末位,比如 0x23,0x45 , 内存从低到高为 0x23,0x45， 按照BigInt的结构,0x23是低位,0x45是高位，也就是为 0x4523,生成的字符串就是倒序,所以需要将 **待加密的数据进行倒序排列才能进行常规加密**  
> 例子: 原值：abcdefg -> 61,62,63,64,65,66,67
> 赋值给BigInt后 内部值 61,62,63,64,65,66,67 
> 表达的大数值为 67666564636261
> JS或者Java大数构造函数需要或生成的字符串为 67666564636261

`k = a.barrett.powMod(j, a.e),` -> 通过RSAKey模幂(a.e==65537(0x010001))计算j得到k  
`16 == a.radix` -> 条件成立,转换为16位Hex流字符串

## 验证

执行Java代码,正确的返回了想要的结果
```
查询语句:{"br":128000,"csrf_token":"","ids":[423227301]}
随机产生的key:T6YgNS1o7HpXVFAn
aes第一次加密:Kyh5bE2z4mxucpwfP8JKlfbU1vSsTiJlJoEu+hJlIkNi4X2NcaKZCJUwx9gSGkVD
aes第二次加密:QrP6dRt4b3dudxKJCQH8GCwyP6Ogr/UmhPoW6lN+j5CJwCFa32Xg2vSWKlJmV+kmV3s3isjAF9Ggs6atBCfBK9IDU2bcRWUjegnXA/B8HMc=
rsa加密后的key为:9bd443934f43cbc16c2d80fef86e850fcfbcddf3744603c6b6b4df8a2dde985e9d62c8c73669a09b7b4035fcbd781665c7553d86436e61b940e611547df5c2691b126fe50d17d0be13648274ce3a461082427adc6aee7166a4ccccc715dcde6be1aa7eb71ed5456de0f4a1adbdd982bd77dee43f9f23bfc1158ebb16e046eab8
POST返回结果:{"data":[{"id":423227301,"url":"http://m10.music.126.net/20161221144746/0bf4e87a2f62c05d431799b25f43997c/ymusic/fd70/1e49/bfec/e2f60e0b4b83de928df2971647ef7f4d.mp3","br":128000,"size":2861392,"md5":"e2f60e0b4b83de928df2971647ef7f4d","code":200,"expi":1200,"type":"mp3","gain":-2.95,"fee":0,"uf":null,"payed":0,"flag":0,"canExtend":false}],"code":200}
**简单解析结果**
id:423227301
url:http://m10.music.126.net/20161221144746/0bf4e87a2f62c05d431799b25f43997c/ymusic/fd70/1e49/bfec/e2f60e0b4b83de928df2971647ef7f4d.mp3
```