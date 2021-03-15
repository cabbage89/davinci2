import React from 'react'
import Row from 'antd/lib/row'
import Col from 'antd/lib/col'
import Checkbox from 'antd/lib/checkbox'
import Select from 'antd/lib/select'
const Option = Select.Option
import InputNumber from 'antd/lib/input-number'
import ColorPicker from 'components/ColorPicker'
import { PIVOT_CHART_YAXIS_OPTIONS, PIVOT_CHART_LINE_STYLES, PIVOT_CHART_FONT_FAMILIES, PIVOT_CHART_FONT_SIZES } from 'app/globalConstants'
const styles = require('../Workbench.less')

export interface IDoubleYAxisConfig {
  yAxisLeft: string
  yAxisRight: string
  leftExtentMin: number,
  leftExtentMax: number
  leftInterval: number
  rightExtentMin: number
  rightExtentMax: number
  rightInterval: number
  yAxisSplitNumber: number
  dataZoomThreshold: number
  inverse: boolean
  showLine: boolean
  lineStyle: 'solid' | 'dashed' | 'dotted'
  lineSize: string
  lineColor: string
  showLabel: boolean
  labelFontFamily: string
  labelFontSize: string
  labelColor: string
}

interface IDoubleYAxisSectionProps {
  title: string
  config: IDoubleYAxisConfig
  onChange: (prop: string, value: any) => void
}

export class DoubleYAxisSection extends React.PureComponent<IDoubleYAxisSectionProps, {}> {
  private selectChange = (prop) => (value) => {
    this.props.onChange(prop, value)
  }

  private inputNumberChange = (prop) => (value) => {
    this.props.onChange(prop, value)
  }

  private checkboxChange = (prop) => (e) => {
    this.props.onChange(prop, e.target.checked)
  }

  private colorChange = (prop) => (color) => {
    this.props.onChange(prop, color)
  }

  public render() {
    const { title, config } = this.props

    const {
      yAxisLeft,
      yAxisRight,
      leftExtentMax,
      leftExtentMin,
      leftInterval,
      rightExtentMax,
      rightExtentMin,
      rightInterval,
      yAxisSplitNumber,
      dataZoomThreshold,
      showLine,
      inverse,
      lineStyle,
      lineSize,
      lineColor,
      showLabel,
      labelFontFamily,
      labelFontSize,
      labelColor
    } = config

    const yAixsOptions = PIVOT_CHART_YAXIS_OPTIONS.map((f) => (
      <Option key={f.value} value={f.value}>{f.name}</Option>
    ))

    const lineStyles = PIVOT_CHART_LINE_STYLES.map((l) => (
      <Option key={l.value} value={l.value}>{l.name}</Option>
    ))
    const fontFamilies = PIVOT_CHART_FONT_FAMILIES.map((f) => (
      <Option key={f.value} value={f.value}>{f.name}</Option>
    ))
    const fontSizes = PIVOT_CHART_FONT_SIZES.map((f) => (
      <Option key={`${f}`} value={`${f}`}>{f}</Option>
    ))

    return (
      <div className={styles.paneBlock}>
        <h4>{title}</h4>
        <div className={styles.blockBody}>
          <Row gutter={8} type="flex" align="middle" className={styles.blockRow}>
            <Col span={10}>左Y轴</Col>
          </Row>
          <Row gutter={8} type="flex" align="middle" className={styles.blockRow}>
            <Col span={10}>
              <Select
                placeholder="yAxisLeft"
                className={styles.blockElm}
                value={yAxisLeft}
                onChange={this.selectChange('yAxisLeft')}
              >
                {yAixsOptions}
              </Select>
            </Col>
            <Col span={10}>
              <InputNumber
                placeholder="刻度间隔"
                className={styles.blockElm}
                value={leftInterval}
                onChange={this.inputNumberChange('leftInterval')}
              />
            </Col>
          </Row>
          <Row gutter={8} type="flex" align="middle" className={styles.blockRow}>
            <Col span={10}>
              <InputNumber
                placeholder="最小值"
                className={styles.blockElm}
                value={leftExtentMin}
                onChange={this.inputNumberChange('leftExtentMin')}
              />
            </Col>
            <Col span={10}>
              <InputNumber
                placeholder="最大值"
                className={styles.blockElm}
                value={leftExtentMax}
                onChange={this.inputNumberChange('leftExtentMax')}
              />
            </Col>
          </Row>
          <Row gutter={8} type="flex" align="middle" className={styles.blockRow}>
            <Col span={10}>右Y轴</Col>
          </Row>
          <Row gutter={8} type="flex" align="middle" className={styles.blockRow}>
            <Col span={10}>
              <Select
                placeholder="yAxisRight"
                className={styles.blockElm}
                value={yAxisRight}
                onChange={this.selectChange('yAxisRight')}
              >
                {yAixsOptions}
              </Select>
            </Col>
            <Col span={10}>
              <InputNumber
                placeholder="刻度间隔"
                className={styles.blockElm}
                value={rightInterval}
                onChange={this.inputNumberChange('rightInterval')}
              />
            </Col>
          </Row>
          <Row gutter={8} type="flex" align="middle" className={styles.blockRow}>
            <Col span={10}>
              <InputNumber
                placeholder="最小值"
                className={styles.blockElm}
                value={rightExtentMin}
                onChange={this.inputNumberChange('rightExtentMin')}
              />
            </Col>
            <Col span={10}>
              <InputNumber
                placeholder="最大值"
                className={styles.blockElm}
                value={rightExtentMax}
                onChange={this.inputNumberChange('rightExtentMax')}
              />
            </Col>
          </Row>
          <Row gutter={8} type="flex" align="middle" className={styles.blockRow}>
            <Col span={12}>
              <Checkbox
                checked={showLine}
                onChange={this.checkboxChange('showLine')}
              >
                显示坐标轴
              </Checkbox>
            </Col>
            <Col span={12}>
              <Checkbox
                checked={inverse}
                onChange={this.checkboxChange('inverse')}
              >
                坐标轴反转
              </Checkbox>
            </Col>
          </Row>
          <Row gutter={8} type="flex" align="middle" className={styles.blockRow}>
            <Col span={10}>
              <Select
                placeholder="样式"
                className={styles.blockElm}
                value={lineStyle}
                onChange={this.selectChange('lineStyle')}
              >
                {lineStyles}
              </Select>
            </Col>
            <Col span={10}>
              <Select
                placeholder="粗细"
                className={styles.blockElm}
                value={lineSize}
                onChange={this.selectChange('lineSize')}
              >
                {Array.from(Array(10), (o, i) => (
                  <Option key={`${i}`} value={`${i + 1}`}>{i + 1}</Option>
                ))}
              </Select>
            </Col>
            <Col span={4}>
              <ColorPicker
                value={lineColor}
                onChange={this.colorChange('lineColor')}
              />
            </Col>
          </Row>
          <Row gutter={8} type="flex" align="middle" className={styles.blockRow}>
            <Col span={24}>
              <Checkbox
                checked={showLabel}
                onChange={this.checkboxChange('showLabel')}
              >
                显示标签文字
              </Checkbox>
            </Col>
          </Row>
          <Row gutter={8} type="flex" align="middle" className={styles.blockRow}>
            <Col span={10}>
              <Select
                placeholder="字体"
                className={styles.blockElm}
                value={labelFontFamily}
                onChange={this.selectChange('labelFontFamily')}
              >
                {fontFamilies}
              </Select>
            </Col>
            <Col span={10}>
              <Select
                placeholder="文字大小"
                className={styles.blockElm}
                value={labelFontSize}
                onChange={this.selectChange('labelFontSize')}
              >
                {fontSizes}
              </Select>
            </Col>
            <Col span={4}>
              <ColorPicker
                value={labelColor}
                onChange={this.colorChange('labelColor')}
              />
            </Col>
          </Row>
        </div>
      </div>
    )
  }
}

export default DoubleYAxisSection
