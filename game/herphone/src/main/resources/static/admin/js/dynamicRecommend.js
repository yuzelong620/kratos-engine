$(document).ready(function() {
	layui.use('table', function() {
		var table = layui.table
			, form = layui.form;

		var count = dynamicRecommendCount();

		table.render({
			elem: '#dynamicRecommend'
			 ,url:'/gm/dynamicRecommend'
			, cellMinWidth: 80
			, limit: 50
			 ,request: {
				 pageName: 'page',   // 页码的参数名称，默认：page
				 limitName: 'count' ,// 每页数据量的参数名，默认：limit
			 }
			 ,parseData:function(res){
				 return{
					 "code":0,
					 "msg":"",
					 "count":count,
					 data:res
				 }
			 }
		    ,where:{
				state:0,
			}
			, cols: [[
				{ type: 'numbers', title: '编号' }
				, { field: 'dynamicId', title: '动态ID', align: 'center', width: 280 }
				, {
					field: 'resList', title: '评论内容', align: 'center', minWidth: 80, templet: function(d) {
						var str = "";
						for(var i = 0;i < d.resList.length;i++){
							if(d.resList[i].type == 1){
								str += "<div>文字内容[  <font color='#FD7E3D'>" + d.resList[i].content + " </font>]</div>"
							}
							if(d.resList[i].type == 2){
								str += "<div style='max-height:100px;max-width:100px; float：left;'><img src=" + d.resList[i].content + "></div>";
							}
							if(d.resList[i].type == 3){
								str += "<video width='150' height='90'  controls>"
								str += "<source src=" + d.resList[i].content + " type='video/mp4'></video>"
							}
						}
						return str;
					}
				}
				, { field: 'recommendPlayerId', title: '推荐人ID', align: 'center', width: 180 }
				, { field: 'recommendPlayerNick', title: '推荐者昵称', align: 'center', width: 160 }
				, { field: 'handlerAdminId', title: '神评人ID', align: 'center', width: 180}
				, { field: 'recommendTime', title: '推荐时间', align: 'center', width: 130 ,templet:function(d){
					return datetimeFormat(d.recommendTime);
				}}
				, { field: '', title: '神评', align: 'center', width: 120, templet: '#switchTpl', unresize: true }
			]]
			, page: {
				layout: ['count', 'prev', 'page', 'next', 'limit', 'refresh', 'skip']
				, limits: [20, 50, 100]
				, first: false
				, last: false
			}
			, done: function(res, curr, count) {
				hoverOpenImg();//显示大图
			}
		});
		
		form.on('switch(sexDemo)', function(obj) {
		if(obj.elem.checked == false) {
			$.ajax({
				url: "/gm/removeDynamicIsBest",
				type: 'get',
				data: {
					dynamicId: this.value
				},
				error: function(XMLHttpRequest, textStatus, errorThrown) {
					if(XMLHttpRequest.status == 401) {
						notLogin(XMLHttpRequest, textStatus, errorThrown);
					} else {
						layer.msg(XMLHttpRequest.responseJSON.message);
					}
				},
				success: function() {
					layer.msg("取消成功");
				}
			});
		} else {
			$.ajax({
				url: "/gm/setDynamicIsBest",
				type: 'get',
				data: {
					dynamicId: this.value
				},
				error: function(XMLHttpRequest, textStatus, errorThrown) {
					if(XMLHttpRequest.status == 401) {
						notLogin(XMLHttpRequest, textStatus, errorThrown);
					} else {
						layer.msg(XMLHttpRequest.responseJSON.message);
					}
				},
				success: function() {
					layer.msg("设置成功");
				}
			});
		}
	});
		
});
});
function dynamicRecommendCount() {
	var num = "";
	$.ajax({
		url: "/gm/dynamicRecommendCount",
		type: 'get',
		async: false,
		error: function(XMLHttpRequest, textStatus, errorThrown) {
			if (XMLHttpRequest.status == 401) {
				notLogin(XMLHttpRequest, textStatus, errorThrown);
			} else {
				layer.msg(XMLHttpRequest.responseJSON.message);
			}
		},
		success: function(result) {
			num = result;
		}
	});
	return num;
}
function hoverOpenImg() {
	var img_show = null;
	$('td img').hover(function() {
		var img = "<img class='img_msg' src='" + $(this).attr('src') + "' style='width:300px;' />";
		img_show = layer.tips(img, this, {
			tips: [2, 'rgba(60,60,60,.9)']
			,time: 100000
			, area: ['330px']
		});
	}, function() {
		layer.close(img_show);
	});
	$('td img').attr('style', 'max-width:70px');
}