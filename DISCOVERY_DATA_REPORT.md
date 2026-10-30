# 新发现公开数据接入记录

验证日期：2026-10-29。新增仓库只读取酷狗公开展示信息，保留原 Node runner 和播放授权流程。

## 已验证官方端点

| 端点 | 使用的数据 | 验证结果 |
| --- | --- | --- |
| `https://m.kugou.com/plist/index&json=true&page=1` | `plist.list.info[]`，当前热门歌单 | HTTP 200，每页 30 个 |
| `https://m.kugou.com/rank/list&json=true` | `rank.list[]`，名称、封面、官方更新时间 | HTTP 200，55 个真实榜单 |
| `https://m.kugou.com/rank/info/?rankid={id}&page=1&json=true` | `info`、`songs.list[]` | 下列榜单均 HTTP 200，每页 30 首 |

歌曲榜：74534 新歌榜、8888 TOP500、6666 飙升榜、30972 酷狗音乐人原创榜。额外验证：24971 DJ 热歌榜、51341 民谣榜、33160 电音榜、31310 欧美榜、31311 韩国榜；官方更新时间均为 2026-10-29。

## 九个分区

- 今日精选：热门歌单前六个，横滑大卡。这里的“今日”表示本次获取的推荐内容，不宣称歌单当天创建或更新。
- 新歌精选：官方新歌榜首 30 首；正在流行：TOP500 首 30 首；热度飙升：飙升榜首 30 首。
- 热门歌单：同一热门歌单列表第 7–18 个，减少和大卡重复。图卡每区最多十二张，大卡最多六张。
- 原创发现：酷狗音乐人原创榜首 30 首。
- 风格漫游：官方目录内的民谣、摇滚、R&B、说唱先锋、爵士、纯音乐、古典、ACG 新歌等榜单。
- 全球声音：官方目录内的欧美、韩国、日本、粤语、香港地区、台湾地区、BillBoard、英国单曲等榜单，保留官方名称。
- 电音与 DJ：官方目录内的 DJ 热歌榜、电音榜、Beatport 电子舞曲榜。

若某榜单从官方目录移除，对应卡片自动省略，不构造空榜或替代榜。歌曲分区存储本页全部有效曲目，详情和播放队列可使用完整 30 首。图卡异步调用 `fetchChartSongs(rankId)` 获取该榜本页歌曲。

## 数据约定和边界

歌曲仅保留 hash、title、artist、artistId、album、albumId、albumAudioId、cover、duration，兼容 `OnlineApi.asTrack`。歌手从官方 authors 数组读取，缺失时使用 filename；专辑名称不从不确定字段推断。播放链接仍由原音乐引擎按用户权益获取。

歌单使用 numeric specialid 作为 id/globalId，listId 为空、type 为 0，匹配原 `playlistSongs` 的公开歌单分支。没有将 `collection_3_*` 标识误用为该接口的 specialid。

原 `recommendations` 包装未转发 category/sort；`artistAlbums` 固定按热门排序，所以本次没有用其结果冒充“本周新发行”。官方移动首页旧 banner（2020 年）未采用；歌曲 addtime 存在遗留常量，也未作为发行日期。没有虚构城市榜、VIP 权益、独家内容、MV 或即将发行栏目。

## 网络与缓存

三个独立工作线程；每分区去重与重试；回调在主线程。详情同步请求不占用仓库线程池。连接超时 8 秒、读取超时 8 秒、响应总读取期限 22 秒，响应上限 1 MiB。仅请求 `https://m.kugou.com`，最多两次同域 HTTPS 跳转；不读取账号凭证，不传账号 Cookie 或 Authorization。封面仅允许已知酷狗图片域名，统一 HTTPS 并丢弃查询参数。

应用 cache 目录 `discovery-public-v1` 每分区一个 JSON，schema/date/fetchedAt 与公开展示 metadata，单文件上限 160 KiB，最多九个（原子写入会短暂存在备份）。20 分钟内同日缓存免重复请求，跨日自动刷新；失败保留至多七天的旧内容并明确 stale/error，保留原始 fetchedAt，不把旧缓存标成当前数据。所有请求可被幂等 `release()` 断开，释放后不再发出 UI 回调。

## 来源

- 官方公开移动站：<https://m.kugou.com/>
- 上游模块实现与接口说明：<https://github.com/MakcRe/KuGouMusicApi/blob/main/docs/README.md>

本次没有修改 runner、原生动态库或设备设置。
