import React from 'react'
import FormType from 'antd/lib/form/Form'
import { createStructuredSelector } from 'reselect'
import RoleForm from './RoleForm'
import CheckPanel from 'app/components/CheckPanel'
// import RelRoleMember from './RelRoleMember'
import { connect } from 'react-redux'
import { Row, Col, Tooltip, Button, Input, Table, Modal, Popconfirm, Divider, message } from 'antd'
import { ColumnProps } from 'antd/lib/table'
const styles = require('../Organization.less')
import { checkNameUniqueAction } from 'containers/App/actions'
import { OrganizationActions } from '../actions'
const { addRole, loadOrganizationRole, deleteRole, relRoleMember, editRole, getRelRoleMember } = OrganizationActions
import ComponentPermission from 'containers/Account/components/checkMemberPermission'
import { makeSelectRoleModalLoading, makeSelectCurrentOrganizationRole } from '../selectors'
import { IOrganization, IOrganizationRole } from '../types'

interface IRoleState {
  formType: string
  formVisible: boolean
  relFormVisible: boolean
  currentRoleId: number
  currentOrganizationRole: any[]
  groupTransfer: { id: string, targets: any[] }
  searchValue: string
  filteredTableSource: {
    type: 'origin' | 'filtered'
    dataSource: any[]
  }
}

interface IRoleProps {
  isLoginUserOwner?: boolean
  onAddRole?: (name: string, desc: string, id: number, resolve: () => any) => any
  onEditRole?: (name: string, desc: string, id: number, resolve: () => any) => any
  onDeleteRole?: (id, resolve) => any
  onRelRoleMember?: (id: number, memberIds: number[], resolve: () => any) => any
  onGetRelRoleMember?: (id: number, resolve: (result: any) => any) => any
  onLoadOrganizationRole?: (orgId: number) => any
  currentOrganization?: IOrganization
  currentOrganizationRole?: IOrganizationRole[]
  organizationMembers?: any[]
  organizations?: any
  roleModalLoading?: boolean
  onLoadOrganizationDetail?: (id: number) => any
  onCheckUniqueName?: (pathname: any, data: any, resolve: () => any, reject: (error: string) => any) => any
}


export interface ITeam {
  id?: number
  role?: number
  avatar?: string
  organization?: IOrganization
  name?: string
  visibility?: boolean
  description: string
  parentTeamId: number
}

export class RoleList extends React.PureComponent<IRoleProps, IRoleState> {
  constructor(props) {
    super(props)
    this.state = {
      formType: 'add',
      formVisible: false,
      relFormVisible: false,
      currentRoleId: 0,
      currentOrganizationRole: [],
      groupTransfer: {
        id: '',
        targets: []
      },
      searchValue: '',
      filteredTableSource: {
        type: 'origin',
        dataSource: []
      }
    }
  }

  private RoleForm: FormType
  private RelRoleMember: FormType

  private refHandles = {
    RoleForm: (ref) => this.RoleForm = ref,
    RelRoleMember: (ref) => this.RelRoleMember = ref
  }

  private showRelRoleForm = (flag, roleId?: number) => (e) => {
    const { onGetRelRoleMember } = this.props
    e.stopPropagation()
    onGetRelRoleMember(roleId, (result) => {
      const targets = result && result.length ? result.map((re) => re.user.id) : []
      // const targets = result && result.length ? result.map((re) => re.user) : []
      this.setState({
        relFormVisible: !this.state.relFormVisible,
        currentRoleId: roleId,
        groupTransfer: {
          id: `${roleId}`,
          targets
        }
      })
    })
  }

  public componentDidMount() {
    const { currentOrganizationRole } = this.props
    if (!currentOrganizationRole) {
      this.loadOrganizationRole()
    } else {
      this.setState({ currentOrganizationRole })
    }
  }

  public componentWillReceiveProps(nextProps) {
    const { currentOrganizationRole } = nextProps
    if (currentOrganizationRole !== this.props.currentOrganizationRole) {
      this.setState({
        currentOrganizationRole
      })
    }
  }

  private loadOrganizationRole = () => this.props.onLoadOrganizationRole(this.props.currentOrganization['id'])

  private showRoleForm = (flag, roleId?: number) => (e) => {
    e.stopPropagation()
    this.setState({
      formVisible: !this.state.formVisible,
      formType: flag
    }, () => {
      if (flag !== 'add') {
        setTimeout(() => {
          const { description, id, name } = this.props.currentOrganizationRole.find((role) => role.id === roleId)
          this.RoleForm.props.form.setFieldsValue({ description, id, name })
        }, 0)
      }
    })
  }


  private checkNameUnique = (rule, value = '', callback) => {
    const { onCheckUniqueName, currentOrganization: { id } } = this.props
    const data = {
      name: value,
      orgId: id,
      id: null
    }
    onCheckUniqueName('team', data,
      () => {
        callback()
      }, (err) => {
        callback(err)
      })
  }

  private onSaveRelRowMember = () => {
    const { onRelRoleMember } = this.props
    const { currentRoleId, groupTransfer } = this.state
    onRelRoleMember(currentRoleId, groupTransfer.targets, () => {
      this.hideRelForm()
    })
  }

  private hideTeamForm = () => {
    this.setState({
      formVisible: false
    })
  }

  private hideRelForm = () => {
    this.setState({
      relFormVisible: false
    })
  }

  private afterTeamFormClose = () => {
    this.RoleForm.props.form.resetFields()
  }

  // private afterRelFormClose = () => {
  //   this.RelRoleMember.props.form.resetFields()
  // }

  private handleDelete = (roleId) => () => {
    const { onDeleteRole } = this.props
    this.loadOrganizationRole()
    if (roleId) {
      onDeleteRole(roleId, () => {
        message.success('????????????')
        this.loadOrganizationRole()
      })
    }
  }

  private onGroupTransferChange = (targets) => {
    this.setState({
      groupTransfer: {
        id: this.state.groupTransfer.id,
        targets
      }
    })
  }

  private searchChange = (e) => {
    const searchValue = e.target.value
    const { currentOrganizationRole } = this.props
    const result = (currentOrganizationRole as any[]).filter((role) => {
      return role && role.name.indexOf(searchValue.trim()) > -1
    })
    this.setState({
      searchValue,
      currentOrganizationRole: searchValue && searchValue.length ? result : this.props.currentOrganizationRole
    })
  }

  private createOrRole = () => {
    const { formType } = this.state
    const { onAddRole, onEditRole } = this.props
    this.RoleForm.props.form.validateFieldsAndScroll((err, values) => {
      if (!err) {
        const { name, description, id } = values
        const orgId = this.props.currentOrganization.id
        if (formType === 'add') {
          onAddRole(name, description, orgId, () => {
            this.loadOrganizationRole()
            this.hideTeamForm()
          })
        } else {
          onEditRole(name, description, id, () => {
            this.loadOrganizationRole()
            this.hideTeamForm()
          })
        }
      }
    })
  }

  public render() {
    const { formVisible, relFormVisible, searchValue, filteredTableSource, formType, groupTransfer, currentOrganizationRole } = this.state
    const { isLoginUserOwner, currentOrganization, currentOrganization: { id }, roleModalLoading, organizationMembers } = this.props
    const roleModalTitle = formType === 'add' ? '????????????' : '??????????????????'
    const relRoleModalTitle = '????????????'
    let CreateButton = void 0
    if (currentOrganization) {
      CreateButton = ComponentPermission(currentOrganization, '')(Button)
    }

    let columns: Array<ColumnProps<any>> = [
      {
        title: '?????????',
        dataIndex: 'name',
        key: 'name'
      },
      {
        title: '??????',
        dataIndex: 'description',
        key: 'description'
      }
    ]

    if (isLoginUserOwner) {
      columns = columns.concat({
        title: '??????',
        dataIndex: 'setting',
        key: 'setting',
        render: (text, record) => (
          <span>
            <a href="javascript:;" onClick={this.showRelRoleForm('add', record.id)}>????????????</a>
            <Divider type="vertical" />
            <a href="javascript:;" onClick={this.showRoleForm('edit', record.id)}>??????</a>
            <Divider type="vertical" />
            <Popconfirm title="???????????????" onConfirm={this.handleDelete(record.id)}>
              <a href="javascript:;">??????</a>
            </Popconfirm>
          </span>
        )
      })
    }

    const addModalButtons =
      (
        <Button
          key="submit"
          type="primary"
          loading={roleModalLoading}
          disabled={roleModalLoading}
          onClick={this.createOrRole}
        >
          ??? ???
        </Button>
      )

    const relRoleModalButtons =
      (
        <Button
          key="submit"
          type="primary"
          loading={roleModalLoading}
          disabled={roleModalLoading}
          onClick={this.onSaveRelRowMember}
        >
          ??? ???
        </Button>
      )
    
    const roleMembers = organizationMembers.map(v => v.user)

    return (
      <div className={styles.listWrapper}>
        <Row>
          <Col span={16}>
            <Input.Search
              value={searchValue}
              placeholder="????????????"
              onChange={this.searchChange}
            />
          </Col>
          <Col span={1} offset={7}>
            <Tooltip placement="bottom" title="????????????">
              <CreateButton
                type="primary"
                icon="plus"
                onClick={this.showRoleForm('add')}
              />
            </Tooltip>
          </Col>
        </Row>
        <Row>
          <div className={styles.tableWrap}>
            <Table
              bordered
              columns={columns}
              dataSource={currentOrganizationRole}
            />
          </div>
        </Row>
        <Modal
          title={roleModalTitle}
          visible={formVisible}
          footer={addModalButtons}
          onCancel={this.hideTeamForm}
          afterClose={this.afterTeamFormClose}
        >
          <RoleForm
            type={formType}
            groupSource={[]}
            organizationMembers={organizationMembers}
            groupTarget={groupTransfer.targets}
            onGroupChange={this.onGroupTransferChange}
            wrappedComponentRef={this.refHandles.RoleForm}
          />
        </Modal>

        <Modal
          width={1040}
          title={relRoleModalTitle}
          visible={relFormVisible}
          footer={relRoleModalButtons}
          onCancel={this.hideRelForm}
          destroyOnClose={true}
        // afterClose={this.afterRelFormClose}
        >
          {/* <RelRoleMember
            organizationMembers={organizationMembers}
            groupTarget={groupTransfer.targets}
            onGroupChange={this.onGroupTransferChange}
            wrappedComponentRef={this.refHandles.RelRoleMember}
          /> */}
          <CheckPanel
            dataSource={roleMembers}
            defaultKeys={groupTransfer.targets}
            closableByTag={true}
            labelKey={'username'}
            valueKey={'id'}
            placeholder={'??????????????????'}
            tokenSeparators={[';']}
            onChange={this.onGroupTransferChange}
          />
        </Modal>
      </div>
    )
  }
}

type MappedStates = ReturnType<typeof mapStateToProps>
type MappedDispatches = ReturnType<typeof mapDispatchToProps>

const mapStateToProps = createStructuredSelector({
  roleModalLoading: makeSelectRoleModalLoading(),
  currentOrganizationRole: makeSelectCurrentOrganizationRole(),
})

export function mapDispatchToProps(dispatch) {
  return {
    onLoadOrganizationRole: (orgId) => dispatch(loadOrganizationRole(orgId)),
    onAddRole: (name, desc, id, resolve) => dispatch(addRole(name, desc, id, resolve)),
    onEditRole: (name, desc, id, resolve) => dispatch(editRole(name, desc, id, resolve)),
    onDeleteRole: (id, resolve) => dispatch(deleteRole(id, resolve)),
    onRelRoleMember: (id, memberIds, resolve) => dispatch(relRoleMember(id, memberIds, resolve)),
    onGetRelRoleMember: (id, resolve) => dispatch(getRelRoleMember(id, resolve)),
    onCheckUniqueName: (pathname, data, resolve, reject) => dispatch(checkNameUniqueAction(pathname, data, resolve, reject))
  }
}

export default connect<MappedStates, MappedDispatches, IRoleProps>(mapStateToProps, mapDispatchToProps)(RoleList)


