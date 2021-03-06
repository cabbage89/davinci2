import React from 'react'
import classnames from 'classnames'
import { findDOMNode } from 'react-dom'

import { Icon, Row, Col, Form, Input, Checkbox, Select, Button, Popover, Modal, message } from 'antd'
import { FormComponentProps } from 'antd/lib/form'
import { WrappedFormUtils } from 'antd/lib/form/Form'
import { CheckboxChangeEvent } from 'antd/lib/checkbox'
const FormItem = Form.Item
const { TextArea } = Input
const { Option } = Select

import 'codemirror/lib/codemirror.css'
import 'assets/override/codemirror_theme.css'
import 'codemirror/addon/hint/show-hint.css'
const codeMirror = require('codemirror/lib/codemirror')
require('codemirror/addon/edit/matchbrackets')
require('codemirror/mode/javascript/javascript')
require('codemirror/addon/hint/show-hint')
require('codemirror/addon/hint/javascript-hint')
require('codemirror/addon/display/placeholder')

import { IFieldConfig } from './types'
import { getDefaultFieldConfig, extractQueryVariableNames, getFieldAlias } from './util'
import { IQueryVariableMap } from 'containers/Dashboard/types'
import AliasExpressionTestModal from './AliasExpressionTest'

const utilStyles = require('assets/less/util.less')

interface IFieldConfigProps extends FormComponentProps {
  visible: boolean
  fieldConfig: IFieldConfig
  queryInfo: string[]
  onSave: (config: IFieldConfig) => void
  onCancel: () => void
}

interface IFieldConfigStates {
  localConfig: IFieldConfig
  queryVariableNames: string[]
  testResult: string
  testModalVisible: boolean
}

class FieldConfig extends React.PureComponent<IFieldConfigProps, IFieldConfigStates> {

  private codeEditor = React.createRef<any>()
  private codeMirrorInst: any
  private isInitEditor: boolean = true

  constructor (props: IFieldConfigProps) {
    super(props)
    const { fieldConfig } = this.props
    this.state = {
      localConfig: fieldConfig ? { ...fieldConfig } : getDefaultFieldConfig(),
      queryVariableNames: [],
      testResult: '',
      testModalVisible: false
    }
  }

  public componentDidMount () {
    const { form } = this.props
    const { localConfig } = this.state
    this.setFieldsValue(form, localConfig)
  }

  public componentDidUpdate () {
    if (this.state.localConfig.useExpression) {
      // @FIXME refactor to remove setTimeout usage
      const tId = setTimeout(() => {
        this.isInitEditor = this.initCodeEditor(this.isInitEditor)
        clearTimeout(tId)
      }, 0)
    }
  }

  public componentWillReceiveProps (nextProps: IFieldConfigProps) {
    const { fieldConfig, form } = nextProps
    if (fieldConfig !== this.props.fieldConfig) {
      this.isInitEditor = true
      form.resetFields()
      this.setState({
        localConfig: fieldConfig ? { ...fieldConfig } : getDefaultFieldConfig(),
        queryVariableNames: [],
        testResult: ''
      }, () => {
        this.setFieldsValue(form, this.state.localConfig)
      })
    }
  }

  private useExpressionChange = (e: CheckboxChangeEvent) => {
    const useExpression = e.target.checked
    this.setState({
      localConfig: {
        ...this.state.localConfig,
        useExpression
      }
    })
  }

  private setFieldsValue = (form: WrappedFormUtils, config: IFieldConfig) => {
    const { alias, desc, useExpression } = config
    form.setFieldsValue({
      [`alias_${useExpression ? 1 : 0}`]: alias,
      desc,
      useExpression
    })
  }

  private getFieldsValue (form: WrappedFormUtils): IFieldConfig {
    const fieldsValue: any = form.getFieldsValue()
    const { useExpression, desc } = fieldsValue
    const alias = useExpression
      ? this.codeMirrorInst.doc.getValue()
      : fieldsValue['alias_0']
    const config: IFieldConfig = {
      alias,
      useExpression,
      desc
    }
    return config
  }

  private initCodeEditor = (isInit: boolean) => {
    if (!this.codeMirrorInst) {
      // @FIXME ref is null in componentDidUpdate
      if (!this.codeEditor.current) { return true }
      const codeEditorDom = findDOMNode(this.codeEditor.current)
      this.codeMirrorInst = codeMirror.fromTextArea(codeEditorDom, {
        mode: 'text/javascript',
        theme: '3024-day',
        lineNumbers: true,
        lineWrapping: true
      })
      this.codeMirrorInst.setSize('100%', 300)
    }
    if (isInit && this.state.localConfig.useExpression) {
      this.codeMirrorInst.doc.setValue(this.state.localConfig.alias)
    }
    return false
  }

  private addQueryVariable = () => {
    const { form } = this.props
    const queryVariable = form.getFieldValue('queryVariable')
    this.codeMirrorInst.replaceSelection(` $${queryVariable}$ `)
  }

  private testExpression = () => {
    const expression: string = this.codeMirrorInst.doc.getValue()
    const queryVariableNames = extractQueryVariableNames(expression)
    const testModalVisible = queryVariableNames.length > 0
    this.setState({
      queryVariableNames,
      testModalVisible
    })
    if (!testModalVisible) {
      this.testExpressionResult()
    }
  }

  private testExpressionResult = (queryVariableMap: IQueryVariableMap = {}) => {
    const { form } = this.props
    const fieldConfig = this.getFieldsValue(form)
    const testResult = getFieldAlias(fieldConfig, queryVariableMap)
    this.setState({
      testResult,
      testModalVisible: false
    })
    return testResult
  }

  private clearExpressionResult = () => {
    this.setState({
      testResult: ''
    })
  }

  private closeTestModal = () => {
    this.setState({ testModalVisible: false })
  }

  private save = () => {
    const { form, onSave } = this.props
    form.validateFieldsAndScroll((err) => {
      if (err) { return }
      const config = this.getFieldsValue(form)
      onSave(config)
    })
  }

  private cancel = () => {
    this.props.onCancel()
  }

  private modalFooter = [(
    <Button
      key="cancel"
      size="large"
      onClick={this.cancel}
    >
      ??? ???
    </Button>
  ), (
    <Button
      key="submit"
      size="large"
      type="primary"
      onClick={this.save}
    >
      ??? ???
    </Button>
  )]

  private useExprInstr = (
    <div>
      <pre>
{`1. ?????????????????? JavaScript Function ??????????????????????????? return ????????????
2. ?????????????????? Widget ????????? View ????????????????????? queryVar ??????????????????????????????????????????????????????
  ???????????? Widget ???????????????????????????????????????????????????????????????????????????????????????????????????
3. ???????????????????????????????????????????????????????????????????????????
  Moment() // ???????????????
  Moment('2018-01-01') // ?????????????????????
  Moment().add(1, 'days').add(-1, 'months') // ????????????????????????????????????????????????????????????????????????'hours', days', 'weeks', 'months', 'years' ??????
  Moment().format('YYYY-MM-DD HH:mm:ss') // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
4. ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
5. ??????
  var province = $province$ // $province ??? View ???????????????
  var currentYearMonth = Moment().format('YYYY???MM???') // ?????????????????????
  var alias = province + '(' + currentYearMonth + ')' // ???????????????????????????
  return alias
`}
      </pre>
    </div>
  )

  public render () {
    const { visible, queryInfo, form } = this.props
    const { getFieldDecorator } = form
    const { localConfig, testResult, testModalVisible, queryVariableNames } = this.state
    const { desc, useExpression } = localConfig
    const variableOptions = (queryInfo || []).map((q) => (
      <Option key={q} value={q}>{q}</Option>
    ))

    const textAreaCls = classnames({ [utilStyles.hide]: !useExpression })
    const inputCls = classnames({ [utilStyles.hide]: useExpression })

    return (
      <Modal
        title="????????????"
        wrapClassName="ant-modal-medium"
        footer={this.modalFooter}
        visible={visible}
        maskClosable={false}
        onCancel={this.cancel}
      >
        <Form>
          <FormItem label="????????????" className={inputCls}>
            {getFieldDecorator('alias_0')(<Input />)}
          </FormItem>
          <FormItem label="????????????" className={textAreaCls} style={{ height: '325px' }}>
            <TextArea ref={this.codeEditor} placeholder="????????????????????????" />
          </FormItem>
          {useExpression && <Row type="flex" align="middle" gutter={8}>
            {variableOptions.length > 0 && (
              <>
                <Col span={9}>
                  <FormItem label="????????????" labelCol={{span: 8}} wrapperCol={{span: 16}}>
                    {getFieldDecorator('queryVariable', {
                      initialValue: queryInfo[0]
                    })(
                      <Select>
                        {variableOptions}
                      </Select>
                    )}
                  </FormItem>
                </Col>
                <Col span={4}>
                  <Row type="flex" align="middle">
                    <FormItem>
                      <Button onClick={this.addQueryVariable}>??????</Button>
                    </FormItem>
                  </Row>
                </Col>
              </>
            )}
            <Col span={3}>
              <FormItem>
                <Button type="primary" onClick={this.testExpression}>??????</Button>
              </FormItem>
            </Col>
            <Col span={8}>
              <FormItem>
                <Input
                  readOnly
                  placeholder="????????????"
                  value={testResult}
                  addonAfter={<Icon type="close" onClick={this.clearExpressionResult} title="????????????" />}
                />
              </FormItem>
            </Col>
          </Row>}
          <Row gutter={8} align="middle" justify="center">
            <Col span={9}>
              <FormItem>
                {getFieldDecorator('useExpression', {
                  initialValue: useExpression,
                  valuePropName: 'checked'
                })(<Checkbox onChange={this.useExpressionChange}>????????????</Checkbox>)}
                <Popover
                  title="????????????????????????"
                  content={this.useExprInstr}
                >
                  <Icon type="info-circle" />
                </Popover>
              </FormItem>
            </Col>
          </Row>
          <FormItem label="????????????">
            {getFieldDecorator('desc', {
              initialValue: desc
            })(<TextArea rows={4} />)}
          </FormItem>
        </Form>
        <AliasExpressionTestModal
          visible={testModalVisible}
          queryVariableNames={queryVariableNames}
          onClose={this.closeTestModal}
          onTest={this.testExpressionResult}
        />
      </Modal>
    )
  }
}

export default Form.create<IFieldConfigProps>()(FieldConfig)
