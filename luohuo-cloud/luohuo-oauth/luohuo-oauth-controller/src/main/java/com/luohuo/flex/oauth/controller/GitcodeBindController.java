package com.luohuo.flex.oauth.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.luohuo.basic.base.R;
import com.luohuo.basic.exception.BizException;
import com.luohuo.flex.base.entity.tenant.DefUser;
import com.luohuo.flex.base.service.tenant.DefUserService;
import com.luohuo.flex.oauth.service.GitcodeAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping
@Tag(name = "第三方绑定", description = "第三方绑定")
@RequiredArgsConstructor
public class GitcodeBindController {

	private final GitcodeAuthService gitcodeAuthService;
	private final DefUserService defUserService;

	@PostMapping("/anyTenant/gitcode/bind")
	@Operation(summary = "绑定 GitCode 账号")
	public R<Boolean> bindGitcode(@RequestParam("code") String code) {
		long userId = StpUtil.getLoginIdAsLong();

		JSONObject userJson = gitcodeAuthService.getGitcodeUserInfo(code);
		Integer id = userJson.getInt("id");
		if (id == null) {
			throw new BizException("获取 GitCode 用户 ID 失败");
		}
		String gitcodeOpenId = String.valueOf(id);

		long count = defUserService.getSuperManager().count(Wrappers.<DefUser>lambdaQuery().eq(DefUser::getGitcodeOpenId, gitcodeOpenId));
		if (count > 0) {
			throw new BizException("该 GitCode 账号已被绑定，请先解绑");
		}

		boolean update = defUserService.getSuperManager().update(Wrappers.<DefUser>lambdaUpdate()
				.set(DefUser::getGitcodeOpenId, gitcodeOpenId)
				.eq(DefUser::getId, userId));

		if (!update) {
			throw new BizException("绑定失败，用户不存在");
		}

		return R.success(true);
	}
}