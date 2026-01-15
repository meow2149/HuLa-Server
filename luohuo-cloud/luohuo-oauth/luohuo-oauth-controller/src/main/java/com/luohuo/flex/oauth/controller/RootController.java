package com.luohuo.flex.oauth.controller;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.luohuo.basic.annotation.log.WebLog;
import com.luohuo.basic.annotation.user.LoginUser;
import com.luohuo.basic.boot.utils.WebUtils;
import com.luohuo.basic.tenant.core.aop.TenantIgnore;
import com.luohuo.flex.base.vo.update.tenant.DefUserPasswordUpdateVO;
import com.luohuo.flex.model.entity.system.SysUser;
import com.luohuo.flex.oauth.granter.QrCodeGranter;
import com.luohuo.flex.oauth.vo.param.ConfirmReq;
import com.luohuo.flex.oauth.vo.param.QueryStatusReq;
import com.luohuo.flex.oauth.vo.param.RefreshTokenVO;
import com.luohuo.flex.oauth.vo.param.ScanReq;
import com.luohuo.flex.oauth.vo.result.QrCodeResp;
import com.luohuo.flex.oauth.vo.result.ScanResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.luohuo.basic.base.R;
import com.luohuo.basic.exception.BizException;
import com.luohuo.flex.base.service.tenant.DefUserService;
import com.luohuo.flex.oauth.enumeration.GrantType;
import com.luohuo.flex.oauth.granter.RefreshTokenGranter;
import com.luohuo.flex.oauth.granter.TokenGranterBuilder;
import com.luohuo.flex.oauth.service.UserInfoService;
import com.luohuo.flex.oauth.vo.param.LoginParamVO;
import com.luohuo.flex.oauth.vo.param.RegisterByEmailVO;
import com.luohuo.flex.oauth.vo.param.RegisterByMobileVO;
import com.luohuo.flex.oauth.vo.result.LoginResultVO;
import org.springframework.beans.factory.annotation.Value;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 登录页 Controller
 *
 * @author 乾乾
 * @date 2020年03月31日10:10:36
 */
@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "登录-退出-注册")
public class RootController {

	private final QrCodeGranter qrCodeGranter;
	private final TokenGranterBuilder tokenGranterBuilder;
	private final RefreshTokenGranter refreshTokenGranter;
	private final DefUserService defUserService;
	private final UserInfoService userInfoService;

	@Value("${gitee.client-id}")
	private String giteeClientId;
	@Value("${gitee.redirect-uri:}")
	private String defaultRedirectUri;
	@Value("${github.client-id}")
	private String githubClientId;
	@Value("${github.redirect-uri:}")
	private String githubRedirectUri;
	@Value("${gitcode.client-id:}")
	private String gitcodeClientId;
	@Value("${gitcode.redirect-uri:}")
	private String gitcodeRedirectUri;

	/**
	 * 登录接口
	 * grantType 表示登录类型 可选值为：CAPTCHA,REFRESH_TOKEN,PASSWORD,MOBILE
	 * @author 乾乾
	 * @date 2025/06/06 9:33 PM
	 */
	@PostMapping(value = "/anyTenant/login")
	@Operation(summary = "登录接口", description = "登录或者清空缓存时调用")
	@TenantIgnore
	public R<LoginResultVO> login(@Validated @RequestBody LoginParamVO login) throws BizException {
		return tokenGranterBuilder.getGranter(login.getGrantType()).login(login);
	}

	@GetMapping("/anyTenant/gitee/authorize-url")
	@Operation(summary = "获取 Gitee 授权地址")
	@TenantIgnore
	public R<String> giteeAuthorizeUrl(@RequestParam(value = "redirect", required = false) String redirect) {
		String redirectUri = defaultRedirectUri;
		if (StrUtil.isNotBlank(redirectUri)) {
			redirectUri = redirectUri.trim();
			if (redirectUri.endsWith("/")) {
				redirectUri = StrUtil.subBefore(redirectUri, "/", true);
			}
		}

		String encoded = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
		String url = "https://gitee.com/oauth/authorize?client_id=" + giteeClientId + "&redirect_uri=" + encoded + "&response_type=code" +
				(StrUtil.isNotBlank(redirect) ? "&state=" + URLEncoder.encode(redirect, StandardCharsets.UTF_8) : "");
		log.info("Generated Gitee Auth URL: {}, redirectUri used: {}", url, redirectUri);
		return R.success(url);
	}

	@GetMapping("/anyTenant/gitee/callback")
	@Operation(summary = "Gitee 授权回调")
	@TenantIgnore
	public void giteeCallback(@RequestParam("code") String code,
							  @RequestParam(value = "state", required = false) String state,
							  @RequestParam(value = "redirect", required = false) String redirect,
							  HttpServletResponse response) throws IOException {
		WebUtils.request().setAttribute("OAUTH_INTERNAL_CALLBACK", true);
		if (StrUtil.isBlank(redirect) && StrUtil.isNotBlank(state)) {
			redirect = state;
		}
		LoginParamVO login = LoginParamVO.builder()
				.grantType(GrantType.GITEE)
				.code(code)
				.systemType(2)
				.deviceType("PC")
				.clientId("WEB")
				.build();
		R<LoginResultVO> result = tokenGranterBuilder.getGranter(login.getGrantType()).login(login);
		if (!result.getsuccess()) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write("{\"success\":false,\"msg\":\"" + result.getMsg() + "\"}");
			return;
		}
		LoginResultVO vo = result.getData();
		if (StrUtil.isNotBlank(redirect)) {
			String location = redirect + "?token=" + URLEncoder.encode(vo.getToken(), StandardCharsets.UTF_8)
					+ "&refreshToken=" + URLEncoder.encode(vo.getRefreshToken(), StandardCharsets.UTF_8)
					+ "&uid=" + URLEncoder.encode(String.valueOf(vo.getUid()), StandardCharsets.UTF_8)
					+ (StrUtil.isNotBlank(state) ? "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8) : "");
			response.setStatus(HttpServletResponse.SC_FOUND);
			response.setHeader("Location", location);
		} else {
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write("{\"success\":true,\"token\":\"" + vo.getToken() + "\",\"refreshToken\":\"" + vo.getRefreshToken() + "\",\"uid\":\"" + vo.getUid() + "\"}");
		}
	}

	@GetMapping("/anyTenant/gitcode/callback")
	@Operation(summary = "GitCode 授权回调")
	@TenantIgnore
	public void gitcodeCallback(@RequestParam("code") String code,
								@RequestParam(value = "state", required = false) String state,
								@RequestParam(value = "redirect", required = false) String redirect,
								HttpServletResponse response) throws IOException {
		WebUtils.request().setAttribute("OAUTH_INTERNAL_CALLBACK", true);
		if (StrUtil.isBlank(redirect) && StrUtil.isNotBlank(state)) {
			redirect = state;
		}
		LoginParamVO login = LoginParamVO.builder()
				.grantType(GrantType.GITCODE)
				.code(code)
				.systemType(2)
				.deviceType("PC")
				.clientId("WEB")
				.build();
		R<LoginResultVO> result = tokenGranterBuilder.getGranter(login.getGrantType()).login(login);
		if (!result.getsuccess()) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write("{\"success\":false,\"msg\":\"" + result.getMsg() + "\"}");
			return;
		}
		LoginResultVO vo = result.getData();
		if (StrUtil.isNotBlank(redirect)) {
			String location = redirect + "?token=" + URLEncoder.encode(vo.getToken(), StandardCharsets.UTF_8)
					+ "&refreshToken=" + URLEncoder.encode(vo.getRefreshToken(), StandardCharsets.UTF_8)
					+ "&uid=" + URLEncoder.encode(String.valueOf(vo.getUid()), StandardCharsets.UTF_8)
					+ (StrUtil.isNotBlank(state) ? "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8) : "");
			response.setStatus(HttpServletResponse.SC_FOUND);
			response.setHeader("Location", location);
		} else {
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write("{\"success\":true,\"token\":\"" + vo.getToken() + "\",\"refreshToken\":\"" + vo.getRefreshToken() + "\",\"uid\":\"" + vo.getUid() + "\"}");
		}
	}
	@GetMapping("/anyTenant/github/authorize-url")
	@Operation(summary = "获取 GitHub 授权地址")
	@TenantIgnore
	public R<String> githubAuthorizeUrl(@RequestParam(value = "redirect", required = false) String redirect) {
		String redirectUri = githubRedirectUri;
		if (StrUtil.isNotBlank(redirectUri)) {
			redirectUri = redirectUri.trim();
			if (redirectUri.endsWith("/")) {
				redirectUri = StrUtil.subBefore(redirectUri, "/", true);
			}
		}
		String encoded = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
		String url = "https://github.com/login/oauth/authorize?client_id=" + githubClientId + "&redirect_uri=" + encoded + "&scope=user:email" +
				(StrUtil.isNotBlank(redirect) ? "&state=" + URLEncoder.encode(redirect, StandardCharsets.UTF_8) : "");
		log.info("Generated GitHub Auth URL: {}, redirectUri used: {}", url, redirectUri);
		return R.success(url);
	}

	@GetMapping("/anyTenant/gitcode/authorize-url")
	@Operation(summary = "获取 GitCode 授权地址")
	@TenantIgnore
	public R<String> gitcodeAuthorizeUrl(@RequestParam(value = "redirect", required = false) String redirect) {
		String redirectUri = gitcodeRedirectUri;
		if (StrUtil.isNotBlank(redirectUri)) {
			redirectUri = redirectUri.trim();
			if (redirectUri.endsWith("/")) {
				redirectUri = StrUtil.subBefore(redirectUri, "/", true);
			}
		}
		String encoded = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
		String url = "https://gitcode.com/oauth/authorize?client_id=" + gitcodeClientId + "&redirect_uri=" + encoded + "&response_type=code" +
				(StrUtil.isNotBlank(redirect) ? "&state=" + URLEncoder.encode(redirect, StandardCharsets.UTF_8) : "");
		log.info("Generated GitCode Auth URL: {}, redirectUri used: {}", url, redirectUri);
		return R.success(url);
	}
	@GetMapping("/anyTenant/github/callback")
	@Operation(summary = "GitHub 授权回调")
	@TenantIgnore
	public void githubCallback(@RequestParam("code") String code,
							   @RequestParam(value = "state", required = false) String state,
							   @RequestParam(value = "redirect", required = false) String redirect,
							   HttpServletResponse response) throws IOException {
		WebUtils.request().setAttribute("OAUTH_INTERNAL_CALLBACK", true);
		if (StrUtil.isBlank(redirect) && StrUtil.isNotBlank(state)) {
			redirect = state;
		}
		LoginParamVO login = LoginParamVO.builder()
				.grantType(GrantType.GITHUB)
				.code(code)
				.systemType(2)
				.deviceType("PC")
				.clientId("WEB")
				.build();
		R<LoginResultVO> result = tokenGranterBuilder.getGranter(login.getGrantType()).login(login);
		if (!result.getsuccess()) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write("{\"success\":false,\"msg\":\"" + result.getMsg() + "\"}");
			return;
		}
		LoginResultVO vo = result.getData();
		if (StrUtil.isNotBlank(redirect)) {
			String location = redirect + "?token=" + URLEncoder.encode(vo.getToken(), StandardCharsets.UTF_8)
					+ "&refreshToken=" + URLEncoder.encode(vo.getRefreshToken(), StandardCharsets.UTF_8)
					+ "&uid=" + URLEncoder.encode(String.valueOf(vo.getUid()), StandardCharsets.UTF_8)
					+ (StrUtil.isNotBlank(state) ? "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8) : "");
			response.setStatus(HttpServletResponse.SC_FOUND);
			response.setHeader("Location", location);
		} else {
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write("{\"success\":true,\"token\":\"" + vo.getToken() + "\",\"refreshToken\":\"" + vo.getRefreshToken() + "\",\"uid\":\"" + vo.getUid() + "\"}");
		}
	}

	@Operation(summary = "刷新token", description = "token过期时，刷新token使用")
	@PostMapping("/anyTenant/refresh")
	public R<LoginResultVO> refresh(@RequestBody RefreshTokenVO refreshToken) throws BizException {
		return R.success(refreshTokenGranter.refresh(refreshToken.getRefreshToken()));
	}

	@Operation(summary = "修改密码", description = "修改密码")
	@PutMapping("/anyTenant/password")
	@WebLog("'修改密码:' + #data.id")
	public R<Boolean> updatePassword(@RequestBody @Validated DefUserPasswordUpdateVO data) {
		return R.success(defUserService.updatePassword(data));
	}

	@Operation(summary = "切换部门")
	@PutMapping("/anyone/switchTenantAndOrg")
	public R<LoginResultVO> switchOrg(@RequestParam(required = false) Long orgId, @RequestParam String clientId) {
		return R.success(tokenGranterBuilder.getGranter(GrantType.PASSWORD).switchOrg(orgId, clientId));
	}

	@Operation(summary = "退出", description = "退出")
	@PostMapping("/anyUser/logout")
	public R<Boolean> logout() {
		return tokenGranterBuilder.getGranter().logout();
	}

	@Operation(summary = "验证token是否正确", description = "验证token")
	@GetMapping("/anyTenant/verify")
	public R<SaSession> verify(@RequestParam("token") String token) throws BizException {
		return R.success(StpUtil.getTokenSessionByToken(token));
	}

	@Operation(summary = "根据手机号注册", description = "根据手机号注册")
	@PostMapping("/anyTenant/registerByMobile")
	public R<String> register(@Validated @RequestBody RegisterByMobileVO register) throws BizException {
		return R.success(userInfoService.registerByMobile(register));
	}

	@Operation(summary = "根据邮箱注册", description = "根据邮箱注册")
	@PostMapping("/anyTenant/registerByEmail")
	public R<String> register(@Parameter(hidden = true) @LoginUser(isEmployee = true) SysUser sysUser, @Validated @RequestBody RegisterByEmailVO register) throws BizException {
		return R.success(userInfoService.registerByEmail(sysUser, register));
	}

	@Operation(summary = "检测邮箱是否存在")
	@GetMapping("/anyTenant/checkEmail")
	public R<Boolean> checkEmail(@RequestParam("email") String email) {
		return R.success(userInfoService.checkEmail(email));
	}

	@Operation(summary = "检测手机号是否存在")
	@GetMapping("/anyTenant/checkMobile")
	public R<Boolean> checkMobile(@RequestParam("mobile") String mobile) {
		return R.success(defUserService.checkMobile(mobile, null));
	}

	@GetMapping("/anyTenant/qr/generate")
	@Operation(summary = "生成登录二维码")
	public R<QrCodeResp> generateQRCode() {
		return R.success(qrCodeGranter.generateQRCode());
	}

	@GetMapping("/anyTenant/qr/status/query")
	@Operation(summary = "查询二维码状态")
	public R checkStatus(@Valid QueryStatusReq req) {
		return qrCodeGranter.checkStatus(req);
	}

	@PostMapping("/qrcode/scan")
	@Operation(summary = "扫描登录二维码")
	public R<ScanResp> handleScan(@Valid @RequestBody ScanReq req) {
		return R.success(qrCodeGranter.handleScan(req));
	}

	@PostMapping("/qrcode/confirm")
	@Operation(summary = "用户在手机上确认登录")
	public R<Long> confirmLogin(@Valid @RequestBody ConfirmReq req) {
		return R.success(qrCodeGranter.confirmLogin(req));
	}
}
