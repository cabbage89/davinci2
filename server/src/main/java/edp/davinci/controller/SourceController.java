/*
 * <<
 *  Davinci
 *  ==
 *  Copyright (C) 2016 - 2019 EDP
 *  ==
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  >>
 *
 */

package edp.davinci.controller;

import com.alibaba.druid.util.StringUtils;
import edp.core.annotation.CurrentUser;
import edp.core.model.DBTables;
import edp.core.model.TableInfo;
import edp.davinci.common.controller.BaseController;
import edp.davinci.core.common.Constants;
import edp.davinci.core.common.ResultMap;
import edp.davinci.dto.sourceDto.*;
import edp.davinci.model.Source;
import edp.davinci.model.User;
import edp.davinci.service.SourceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@Api(value = "/sources", tags = "sources", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@ApiResponses(@ApiResponse(code = 404, message = "sources not found"))
@RestController
@RequestMapping(value = Constants.BASE_API_PATH + "/sources", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class SourceController extends BaseController {

    @Autowired
    private SourceService sourceService;


    /**
     * ??????source??????
     *
     * @param projectId
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "get sources")
    @GetMapping
    public ResponseEntity getSources(@RequestParam Long projectId,
                                     @ApiIgnore @CurrentUser User user,
                                     HttpServletRequest request) {
        if (invalidId(projectId)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid project id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        List<Source> sources = sourceService.getSources(projectId, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payloads(sources));
    }


    /**
     * ??????source ??????
     *
     * @param id
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "get source detail")
    @GetMapping("/{id}")
    public ResponseEntity getSourceDetail(@PathVariable Long id,
                                          @ApiIgnore @CurrentUser User user,
                                          HttpServletRequest request) {
        if (invalidId(id)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid project id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        SourceDetail sourceDetail = sourceService.getSourceDetail(id, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(sourceDetail));
    }


    /**
     * ??????source
     *
     * @param source
     * @param bindingResult
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "create source", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity createSource(@Valid @RequestBody SourceCreate source,
                                       @ApiIgnore BindingResult bindingResult,
                                       @ApiIgnore @CurrentUser User user,
                                       HttpServletRequest request) {

        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        Source record = sourceService.createSource(source, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(record));
    }


    /**
     * ??????source
     *
     * @param id
     * @param source
     * @param bindingResult
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "update a source", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity updateSource(@PathVariable Long id,
                                       @Valid @RequestBody SourceInfo source,
                                       @ApiIgnore BindingResult bindingResult,
                                       @ApiIgnore @CurrentUser User user,
                                       HttpServletRequest request) {


        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        if (invalidId(id) || !id.equals(source.getId())) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid source id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        sourceService.updateSource(source, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }

    /**
     * ??????source
     *
     * @param id
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "delete a source")
    @DeleteMapping("/{id}")
    public ResponseEntity deleteSource(@PathVariable Long id,
                                       @ApiIgnore @CurrentUser User user,
                                       HttpServletRequest request) {

        if (invalidId(id)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid source id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        sourceService.deleteSource(id, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }


    /**
     * ????????????
     *
     * @param sourceTest
     * @param bindingResult
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "test source", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(value = "/test", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity testSource(@Valid @RequestBody SourceTest sourceTest,
                                     @ApiIgnore BindingResult bindingResult,
                                     @ApiIgnore @CurrentUser User user,
                                     HttpServletRequest request) {

        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        sourceService.testSource(sourceTest);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }

    /**
     * ????????????
     *
     * @param id
     * @param dbBaseInfo
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "release and reconnect", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(value = "/reconnect/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity reconnect(@PathVariable Long id,
                                    @RequestBody DbBaseInfo dbBaseInfo,
                                    @ApiIgnore @CurrentUser User user,
                                    HttpServletRequest request) {
        sourceService.reconnect(id, dbBaseInfo, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }


    /**
     * ??????csv??????????????????
     *
     * @param id
     * @param uploadMeta
     * @param bindingResult
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "create csv meta", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(value = "{id}/csvmeta", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity createCsvmeta(@PathVariable Long id,
                                        @Valid @RequestBody UploadMeta uploadMeta,
                                        @ApiIgnore BindingResult bindingResult,
                                        @ApiIgnore @CurrentUser User user,
                                        HttpServletRequest request) {

        if (invalidId(id)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid source id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        sourceService.validCsvmeta(id, uploadMeta, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }

    /**
     * ???csv??????
     *
     * @param id
     * @param sourceDataUpload
     * @param bindingResult
     * @param file
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "upload csv/excel file")
    @PostMapping("{id}/upload{type}")
    public ResponseEntity uploadData(@PathVariable Long id,
                                     @PathVariable String type,
                                     @Valid @ModelAttribute(value = "sourceDataUpload") SourceDataUpload sourceDataUpload,
                                     @ApiIgnore BindingResult bindingResult,
                                     @RequestParam("file") MultipartFile file,
                                     @ApiIgnore @CurrentUser User user,
                                     HttpServletRequest request) {

        if (invalidId(id)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid source id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        if (file.isEmpty() || StringUtils.isEmpty(file.getOriginalFilename())) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Upload file can not be empty");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        sourceService.dataUpload(id, sourceDataUpload, file, user, type);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }


    /**
     * source ????????????
     *
     * @param id
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "get dbs")
    @GetMapping("/{id}/databases")
    public ResponseEntity getSourceDbs(@PathVariable Long id,
                                       @ApiIgnore @CurrentUser User user,
                                       HttpServletRequest request) {
        if (invalidId(id)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid source id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        List<String> dbs = sourceService.getSourceDbs(id, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(new SourceCatalogInfo(id, dbs)));
    }


    /**
     * source ???????????????
     *
     * @param id
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "get tables")
    @GetMapping("/{id}/tables")
    public ResponseEntity getSourceTables(@PathVariable Long id,
                                          @RequestParam(name = "dbName") String dbName,
                                          @ApiIgnore @CurrentUser User user,
                                          HttpServletRequest request) {
        if (invalidId(id)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid source id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        DBTables dbTables = sourceService.getSourceTables(id, dbName, user);
        SourceDBInfo dbTableInfo = new SourceDBInfo();
        dbTableInfo.setSourceId(id);
        BeanUtils.copyProperties(dbTables, dbTableInfo);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(dbTableInfo));
    }


    /**
     * ?????????
     *
     * @param id
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "get columns")
    @GetMapping("/{id}/table/columns")
    public ResponseEntity getTableColumns(@PathVariable Long id,
                                          @RequestParam(name = "dbName") String dbName,
                                          @RequestParam(name = "tableName") String tableName,
                                          @ApiIgnore @CurrentUser User user,
                                          HttpServletRequest request) {
        if (invalidId(id)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid source id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        if (StringUtils.isEmpty(tableName)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Table cannot be empty");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        TableInfo tableInfo = sourceService.getTableInfo(id, dbName, tableName, user);

        SourceTableInfo sourceTableInfo = new SourceTableInfo();
        sourceTableInfo.setSourceId(id);
        sourceTableInfo.setTableName(tableName);
        BeanUtils.copyProperties(tableInfo, sourceTableInfo);

        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(sourceTableInfo));
    }


    /**
     * ??????????????????jdbc?????????
     *
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "get jdbc datasources")
    @GetMapping("/jdbc/datasources")
    public ResponseEntity getJdbcDataSources(@ApiIgnore @CurrentUser User user, HttpServletRequest request) {
        List<DatasourceType> list = sourceService.getDatasources();
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payloads(list));
    }

}
