package com.example.guet_map.network

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * 开发阶段 Mock 拦截器：拦截 API 请求并返回样本数据。
 * 接入真实后端后删除此拦截器即可。
 */
class MockInterceptor : Interceptor {

    private val jsonMediaType = "application/json".toMediaType()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        return when {
            path.matches(Regex("/api/v1/locations")) -> {
                mockResponse(request, LOCATIONS_JSON)
            }
            path.matches(Regex("/api/v1/locations/[^/]+")) -> {
                val locationId = path.substringAfterLast("/")
                val locationJson = LOCATION_DETAIL_JSON[locationId]
                    ?: """{"error":"not found","locationId":"$locationId"}"""
                mockResponse(request, locationJson)
            }
            path.matches(Regex("/api/v1/locations/[^/]+/guides")) -> {
                val locationId = path.split("/")[4]
                val guideJson = GUIDE_STEPS_JSON[locationId]
                    ?: GUIDE_STEPS_JSON["default"]!!
                mockResponse(request, guideJson)
            }
            path == "/api/v1/guides/upload" && request.method == "POST" -> {
                mockResponse(request, UPLOAD_RESPONSE_JSON)
            }
            else -> chain.proceed(request)
        }
    }

    private fun mockResponse(request: okhttp3.Request, body: String): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK (Mock)")
            .body(body.toResponseBody(jsonMediaType))
            .build()
    }
}

// ── Mock 数据 ─────────────────────────────────────────────────────

private val LOCATIONS_JSON = """
[
  {"locationId":"library","name":"图书馆","latitude":25.313315,"longitude":110.418835,"category":"图书馆","rating":4.8,"openingHours":"07:00-22:30","imageUrl":"https://example.com/img/library.jpg","hasGuide":true,"address":"灵田乡西岸村桂林电子科技大学花江校区","phone":"0773-2291357","description":"学校主图书馆，设施完善，藏书丰富，是学习的好去处","poiId":"B0FFFVHJMY"},
  {"locationId":"canteen_yayuan","name":"雅园餐厅","latitude":25.308052,"longitude":110.414401,"category":"食堂","rating":4.4,"openingHours":"06:00-21:30","imageUrl":"https://example.com/img/yayuan.jpg","hasGuide":false,"address":"桂林电子科技大学花江校区","phone":"","description":"学校主要餐厅之一，提供多种美食选择","poiId":"B0KGTRFB35"},
  {"locationId":"canteen_yiyuan","name":"怡园餐厅","latitude":25.316468,"longitude":110.416821,"category":"食堂","rating":4.4,"openingHours":"09:00-21:00","imageUrl":"https://example.com/img/yiyuan.jpg","hasGuide":false,"address":"167乡道电子科技大学尧山分校区附近","phone":"","description":"学生活动中心旁的餐厅，环境优美","poiId":"B0FFJ10PNZ"},
  {"locationId":"canteen_central","name":"中央食堂","latitude":25.320275,"longitude":110.415425,"category":"食堂","rating":3.7,"openingHours":"全天","imageUrl":"https://example.com/img/central.jpg","hasGuide":false,"address":"桂林电子科技大学花江校区清苑东北侧110米","phone":"","description":"学校大型食堂，菜品丰富多样","poiId":"B0LKXCOJTT"},
  {"locationId":"canteen_zhongyuan","name":"仲园餐厅","latitude":25.311775,"longitude":110.410425,"category":"食堂","rating":4.3,"openingHours":"全天","imageUrl":"https://example.com/img/zhongyuan.jpg","hasGuide":false,"address":"灵田镇","phone":"","description":"位于校园东侧的餐厅","poiId":"B0FFG07YGH"},
  {"locationId":"gym","name":"体育馆","latitude":25.319390,"longitude":110.416340,"category":"运动场","rating":3.5,"openingHours":"全天","imageUrl":"https://example.com/img/gym.jpg","hasGuide":false,"address":"桂林电子科技大学花江校区清苑东侧130米","phone":"","description":"室内综合体育馆，篮球、排球、羽毛球等场地","poiId":"B0M6XCRM1Y"},
  {"locationId":"football_field","name":"D区足球场","latitude":25.312003,"longitude":110.413887,"category":"运动场","rating":4.0,"openingHours":"全天","imageUrl":"https://example.com/img/football.jpg","hasGuide":false,"address":"桂林电子科技大学花江校区D区篮球场东侧130米","phone":"","description":"标准足球场，足球爱好者的聚集地","poiId":"B0K27Z6W0L"},
  {"locationId":"tennis_indoor","name":"室内网球馆","latitude":25.313910,"longitude":110.412165,"category":"运动场","rating":4.1,"openingHours":"全天","imageUrl":"https://example.com/img/tennis.jpg","hasGuide":false,"address":"灵田镇桂林电子科技大学(花江校区)","phone":"","description":"室内网球馆，提供专业网球场地","poiId":"B0JBHAYOJP"},
  {"locationId":"stu_activity_center","name":"学生活动中心","latitude":25.316054,"longitude":110.415473,"category":"学生活动","rating":4.5,"openingHours":"全天","imageUrl":"https://example.com/img/activity.jpg","hasGuide":false,"address":"桂林电子科技大学花江校区怡园餐厅旁","phone":"0773-2319280","description":"学生社团活动、文艺演出、讲座的场所","poiId":"B0FFKHDMP7"},
  {"locationId":"dorm_qingyuan","name":"清苑宿舍区","latitude":25.319317,"longitude":110.415012,"category":"宿舍","rating":4.2,"openingHours":"全天","imageUrl":"https://example.com/img/qingyuan.jpg","hasGuide":false,"address":"桂林电子科技大学","phone":"0773-2290283","description":"女生宿舍区，环境优美，设施齐全","poiId":"B0FFF4O3BP"},
  {"locationId":"dorm_jingyuan","name":"泾苑宿舍区","latitude":25.317604,"longitude":110.414713,"category":"宿舍","rating":4.0,"openingHours":"全天","imageUrl":"https://example.com/img/jingyuan.jpg","hasGuide":false,"address":"灵田镇桂林电子科技大学男生宿舍A区","phone":"","description":"男生宿舍区A区","poiId":"B0FFH297QO"},
  {"locationId":"dorm_runyuan","name":"润苑宿舍区","latitude":25.311072,"longitude":110.409277,"category":"宿舍","rating":4.1,"openingHours":"全天","imageUrl":"https://example.com/img/runyuan.jpg","hasGuide":false,"address":"桂林电子科技大学花江校区","phone":"","description":"校园北侧宿舍区","poiId":"B0K2KZYM68"},
  {"locationId":"dorm_mengyuan","name":"濛苑宿舍区","latitude":25.307446,"longitude":110.413696,"category":"宿舍","rating":4.0,"openingHours":"全天","imageUrl":"https://example.com/img/mengyuan.jpg","hasGuide":false,"address":"桂林电子科技大学花江校区","phone":"","description":"校园西侧宿舍区","poiId":"B0K2KZYM69"},
  {"locationId":"college_mechatronics","name":"机电工程学院","latitude":25.310882,"longitude":110.421371,"category":"学院","rating":4.3,"openingHours":"工作日 08:00-18:00","imageUrl":"https://example.com/img/mechatronics.jpg","hasGuide":false,"address":"桂林电子科技大学(花江校区)","phone":"","description":"机械设计制造、自动化等专业的教学学院","poiId":"B0FFFY8I4E"},
  {"locationId":"college_computer","name":"信息科技学院","latitude":25.307837,"longitude":110.418729,"category":"学院","rating":4.5,"openingHours":"工作日 08:00-18:00","imageUrl":"https://example.com/img/computer.jpg","hasGuide":false,"address":"灵田镇桂林电子科技大学(花江校区)","phone":"","description":"计算机科学与技术、软件工程等专业的教学学院","poiId":"B0FFF2UH81"},
  {"locationId":"college_arts","name":"艺术与设计学院","latitude":25.307837,"longitude":110.418525,"category":"学院","rating":4.4,"openingHours":"工作日 08:00-18:00","imageUrl":"https://example.com/img/arts.jpg","hasGuide":false,"address":"灵田乡东阳路3号桂林电子科技大学(花江校区)","phone":"","description":"数字媒体技术、工业设计等专业的教学学院","poiId":"B0FFHSP72W"},
  {"locationId":"college_international","name":"国际学院","latitude":25.312375,"longitude":110.424125,"category":"学院","rating":4.2,"openingHours":"工作日 08:00-18:00","imageUrl":"https://example.com/img/international.jpg","hasGuide":false,"address":"灵田镇桂林电子科技大学(花江校区)","phone":"","description":"国际合作办学项目学院","poiId":"B0G2SRKCCI"},
  {"locationId":"college_foreigner","name":"外国语学院","latitude":25.315472,"longitude":110.419273,"category":"学院","rating":4.3,"openingHours":"工作日 08:00-18:00","imageUrl":"https://example.com/img/foreign.jpg","hasGuide":false,"address":"灵田乡东田村桂林电子科技大学花江校区","phone":"","description":"英语、日语、法语等外语专业教学学院","poiId":"B0FFFQO5N6"},
  {"locationId":"college_law","name":"法学院","latitude":25.314390,"longitude":110.414583,"category":"学院","rating":4.1,"openingHours":"工作日 08:00-18:00","imageUrl":"https://example.com/img/law.jpg","hasGuide":false,"address":"灵田乡东田村桂林电子科技大学花江校区","phone":"","description":"法学专业的教学学院","poiId":"B0FFG2BLMO"},
  {"locationId":"college_env","name":"生命与环境科学学院","latitude":25.312088,"longitude":110.418272,"category":"学院","rating":4.2,"openingHours":"工作日 08:00-18:00","imageUrl":"https://example.com/img/env.jpg","hasGuide":false,"address":"桂林电子科技大学-花江校区","phone":"","description":"生物工程、环境科学等专业的教学学院","poiId":"B03050WILO"},
  {"locationId":"graduate_school","name":"研究生院","latitude":25.306954,"longitude":110.416001,"category":"学院","rating":4.4,"openingHours":"工作日 08:00-18:00","imageUrl":"https://example.com/img/graduate.jpg","hasGuide":false,"address":"灵田乡东阳路3号","phone":"0773-2290550","description":"学校研究生教育管理部门所在地","poiId":"B030507U5H"},
  {"locationId":"innovation_base","name":"大学生创新实践基地","latitude":25.307439,"longitude":110.417455,"category":"学院","rating":4.5,"openingHours":"全天","imageUrl":"https://example.com/img/innovation.jpg","hasGuide":false,"address":"桂林电子科技大学花江校区","phone":"","description":"学生创新创业、学科竞赛的实践平台","poiId":"B0FFG17CUG"},
  {"locationId":"robotics_center","name":"机器人中心","latitude":25.315651,"longitude":110.414557,"category":"学院","rating":4.6,"openingHours":"全天","imageUrl":"https://example.com/img/robotics.jpg","hasGuide":false,"address":"桂林电子科技大学(花江校区)","phone":"","description":"机器人研究、创新实验中心","poiId":"B0HR0KME6M"},
  {"locationId":"sihuajingu","name":"花江慧谷","latitude":25.315395,"longitude":110.413739,"category":"学院","rating":4.3,"openingHours":"全天","imageUrl":"https://example.com/img/sihuajingu.jpg","hasGuide":false,"address":"灵田镇桂林电子科技大学(花江校区)","phone":"","description":"校企合作园区，科技创新孵化基地","poiId":"B0H335S98W"},
  {"locationId":"quality_expansion","name":"素质拓展基地","latitude":25.313825,"longitude":110.422075,"category":"运动场","rating":4.2,"openingHours":"全天","imageUrl":"https://example.com/img/quality.jpg","hasGuide":false,"address":"桂林电子科技大学游泳馆北侧120米","phone":"","description":"户外拓展、团队训练基地","poiId":"B0L0HPPZUG"},
  {"locationId":"gate_south","name":"南门","latitude":25.304553,"longitude":110.419324,"category":"校门","rating":4.5,"openingHours":"全天","imageUrl":"https://example.com/img/south_gate.jpg","hasGuide":false,"address":"灵朝线与167乡道交叉口西200米","phone":"","description":"学校正门，主要出入口","poiId":"B0L1412VEB"},
  {"locationId":"sixian_lake","name":"思贤湖","latitude":25.310725,"longitude":110.420046,"category":"景观","rating":4.7,"openingHours":"全天","imageUrl":"https://example.com/img/lake.jpg","hasGuide":false,"address":"桂林电子科技大学游泳馆西南侧280米","phone":"","description":"校园标志性景观湖，环境优美，是散步休闲的好去处","poiId":"B0M6CHUVQW"},
  {"locationId":"express_station","name":"菜鸟驿站","latitude":25.316311,"longitude":110.416495,"category":"快递","rating":4.5,"openingHours":"08:30-20:00","imageUrl":"https://example.com/img/express.jpg","hasGuide":false,"address":"桂林电子科技大学花江校区中央食堂旁","phone":"4001787878","description":"校园快递收发点","poiId":"B0FFJY5FHZ"},
  {"locationId":"kaoyan_training","name":"海天考研","latitude":25.320177,"longitude":110.416396,"category":"培训","rating":4.0,"openingHours":"08:00-23:30","imageUrl":"https://example.com/img/kaoyan.jpg","hasGuide":false,"address":"桂林电子科技大学花江校区清苑东北侧160米","phone":"0773-7798777","description":"考研培训机构","poiId":"B0FFKBQO02"}
]
""".trimIndent()

private val LOCATION_DETAIL_JSON = mapOf(
    "library" to """{"locationId":"library","name":"图书馆","latitude":25.313315,"longitude":110.418835,"category":"图书馆","rating":4.8,"openingHours":"07:00-22:30","imageUrl":"https://example.com/img/library.jpg","hasGuide":true,"address":"灵田乡西岸村桂林电子科技大学花江校区","phone":"0773-2291357","description":"学校主图书馆，设施完善，藏书丰富，是学习的好去处","poiId":"B0FFFVHJMY"}""",
    "gate_south" to """{"locationId":"gate_south","name":"南门","latitude":25.304553,"longitude":110.419324,"category":"校门","rating":4.5,"openingHours":"全天","imageUrl":"https://example.com/img/south_gate.jpg","hasGuide":false,"address":"灵朝线与167乡道交叉口西200米","phone":"","description":"学校正门，主要出入口","poiId":"B0L1412VEB"}""",
    "stu_activity_center" to """{"locationId":"stu_activity_center","name":"学生活动中心","latitude":25.316054,"longitude":110.415473,"category":"学生活动","rating":4.5,"openingHours":"全天","imageUrl":"https://example.com/img/activity.jpg","hasGuide":false,"address":"桂林电子科技大学花江校区怡园餐厅旁","phone":"0773-2319280","description":"学生社团活动、文艺演出、讲座的场所","poiId":"B0FFKHDMP7"}"""
)

private val GUIDE_STEPS_JSON = mapOf(
    "library" to """
[
  {"id":1,"locationId":"library","stepNumber":1,"description":"从南门进入校园，沿主干道直行约800米","imageUrl":"https://example.com/guide/lib_01.jpg"},
  {"id":2,"locationId":"library","stepNumber":2,"description":"经过中央食堂后继续直行","imageUrl":"https://example.com/img/central.jpg"},
  {"id":3,"locationId":"library","stepNumber":3,"description":"看到研究生院大楼后右转","imageUrl":"https://example.com/img/graduate.jpg"},
  {"id":4,"locationId":"library","stepNumber":4,"description":"沿着校园道路直行约200米","imageUrl":"https://example.com/img/path.jpg"},
  {"id":5,"locationId":"library","stepNumber":5,"description":"到达图书馆大楼","imageUrl":"https://example.com/img/library.jpg"}
]
""".trimIndent(),
    "gym" to """
[
  {"id":1,"locationId":"gym","stepNumber":1,"description":"从南门进入校园，沿主干道直行约600米","imageUrl":"https://example.com/guide/gym_01.jpg"},
  {"id":2,"locationId":"gym","stepNumber":2,"description":"经过清苑宿舍区左转","imageUrl":"https://example.com/img/qingyuan.jpg"},
  {"id":3,"locationId":"gym","stepNumber":3,"description":"直行约200米即可看到体育馆","imageUrl":"https://example.com/img/gym.jpg"}
]
""".trimIndent(),
    "default" to """
[
  {"id":1,"locationId":"unknown","stepNumber":1,"description":"暂无详细指引，请联系管理员添加","imageUrl":""}
]
""".trimIndent()
)

private val UPLOAD_RESPONSE_JSON = """
{"success":true,"message":"上传成功，待审核通过后将发放积分","pointsAwarded":5}
""".trimIndent()
