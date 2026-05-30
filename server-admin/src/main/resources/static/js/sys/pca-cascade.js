/**
 * 省市区三级联动（数据来自 /business/sys/pca，不直接读 pca-code.json）
 */
(function (global) {
    function emptyOption(label) {
        return '<option value="">' + (label || '全部') + '</option>';
    }

    function PcaCascade(options) {
        this.$ = options.$ || (typeof layui !== 'undefined' ? layui.jquery : global.jQuery || global.$);
        this.baseUrl = options.baseUrl || '/business/sys/pca';
        this.$province = this.$(options.province);
        this.$city = this.$(options.city);
        this.$district = this.$(options.district);
        this.layForm = options.layForm;
        this.allowEmpty = options.allowEmpty !== false;
        this.initial = options.initial || {};
        this.provinceFilter = options.provinceFilter || 'pcaProvince';
        this.cityFilter = options.cityFilter || 'pcaCity';
        this.districtFilter = options.districtFilter || 'pcaDistrict';
    }

    PcaCascade.prototype.renderSelect = function () {
        if (this.layForm) {
            this.layForm.render('select');
        }
    };

    PcaCascade.prototype.fillSelect = function ($el, list, emptyLabel, selectedCode) {
        var html = '';
        if (this.allowEmpty) {
            html += emptyOption(emptyLabel);
        }
        (list || []).forEach(function (item) {
            var selected = selectedCode && item.code === selectedCode ? ' selected' : '';
            html += '<option value="' + item.code + '"' + selected + '>' + item.name + '</option>';
        });
        $el.html(html);
    };

    PcaCascade.prototype.fetch = function (path, params, done) {
        var self = this;
        self.$.get(self.baseUrl + path, params, function (res) {
            if (res && res.code === 200) {
                done(res.data || []);
            } else {
                done([]);
            }
        }).fail(function () {
            done([]);
        });
    };

    PcaCascade.prototype.resetCityDistrict = function (cityLabel, districtLabel) {
        this.fillSelect(this.$city, [], cityLabel || '请选择市', null);
        this.fillSelect(this.$district, [], districtLabel || '请选择区/县', null);
        this.renderSelect();
    };

    PcaCascade.prototype.loadProvinces = function (selectedProvince) {
        var self = this;
        var provinceCode = selectedProvince || self.initial.province;
        self.fetch('/provinces', {}, function (list) {
            self.fillSelect(self.$province, list, '请选择省', provinceCode);
            self.resetCityDistrict();
            if (provinceCode) {
                self.loadCities(provinceCode, self.initial.city, self.initial.district);
            }
        });
    };

    PcaCascade.prototype.loadCities = function (provinceCode, selectedCity, selectedDistrict) {
        var self = this;
        if (!provinceCode) {
            self.resetCityDistrict();
            return;
        }
        self.fetch('/cities', {provinceCode: provinceCode}, function (list) {
            self.fillSelect(self.$city, list, '请选择市', selectedCity);
            self.fillSelect(self.$district, [], '请选择区/县', null);
            self.renderSelect();
            if (selectedCity) {
                self.loadDistricts(selectedCity, selectedDistrict);
            }
        });
    };

    PcaCascade.prototype.loadDistricts = function (cityCode, selectedDistrict) {
        var self = this;
        if (!cityCode) {
            self.fillSelect(self.$district, [], '请选择区/县', null);
            self.renderSelect();
            return;
        }
        self.fetch('/districts', {cityCode: cityCode}, function (list) {
            self.fillSelect(self.$district, list, '请选择区/县', selectedDistrict);
            self.renderSelect();
        });
    };

    PcaCascade.prototype.bindEvents = function () {
        var self = this;
        if (self.layForm) {
            self.layForm.on('select(' + self.provinceFilter + ')', function (data) {
                self.loadCities(data.value, null, null);
            });
            self.layForm.on('select(' + self.cityFilter + ')', function (data) {
                self.loadDistricts(data.value, null);
            });
            return;
        }
        self.$province.off('change.pca').on('change.pca', function () {
            self.loadCities(self.$(this).val(), null, null);
        });
        self.$city.off('change.pca').on('change.pca', function () {
            self.loadDistricts(self.$(this).val(), null);
        });
    };

    PcaCascade.prototype.init = function () {
        this.bindEvents();
        this.loadProvinces(this.initial.province);
    };

    global.PcaCascade = PcaCascade;
    /** @deprecated 使用 PcaCascade */
    global.TeaPcaCascade = PcaCascade;
})(window);
