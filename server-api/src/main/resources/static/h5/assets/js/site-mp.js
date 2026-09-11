/**
 * 标题旁公众号推广图：挂载到 #siteMp。
 * 图：/h5/assets/image/rcch_mp.png（含搜一搜 + 二维码，无额外文案）
 */
(function () {
  var mount = document.getElementById("siteMp");
  if (!mount) {
    return;
  }
  var img = document.createElement("img");
  img.src = "/h5/assets/image/rcch_mp.png";
  img.alt = "关注榕城茶话";
  img.className = "site-mp-img";
  mount.appendChild(img);
})();
