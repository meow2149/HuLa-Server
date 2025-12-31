# luohuo-validator-starter 使用说明

## 测试代码位置

测试代码请查看：`luohuo-authority-controller -> test/*ValidateController`

## 支持的入参类型

`hibernate-validator` 官方对以下 3 种入参类型都支持校验，但目前本工具类 **只支持获取第 2、3 种类型** 的入参校验规则：

- **普通参数类型**（示例：`ParamValidateController`）

```java
@Validated
public class ParamValidateController {
    @GetMapping("/requestParam/get1")
    public String paramGet1(@NotEmpty(message = "不能为空")
                            @RequestParam(value = "code", required = false) String code) {
        return "一定要在类上面写@Validated注解";
    }
}
```

- **对象参数**（示例：`ObjValidateController`）

```java
@GetMapping("/obj/get3")
public String objGet3(@Validated InnerDTO data) {
    return "只有参数上有@Validated 可以验证";
}
```

- **`@RequestBody` 对象参数**（示例：`BodyValidateController`）

```java
@PostMapping("/post6")
public String bodyPost6(@Validated @RequestBody HiberDTO data) {
    return "只有参数上有@Validated， 可以验证";
}
```

## 如何使用

参考文档：[zuihou-admin-cloud 校验相关说明](https://www.kancloud.cn/zuihou/zuihou-admin-cloud/2074606)
