// TICKET-ADV123 — React Hook Form + Yup validation.
import React from 'react';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { withAuth } from '@components/withAuth.jsx';
import { api } from '@services/apiService.js';

// TODO(TICKET-ADV123): build a yup.object schema covering every field on the
//   form. Suggested validators:
//     tradeRef       — string, regex /^[A-Z]{3}-\d{8}-\d{4}$/ ("AAA-YYYYMMDD-NNNN")
//     instrumentId   — integer, positive
//     counterpartyId — integer, positive
//     assetClass     — oneOf ['EQUITY','FX','BOND','DERIVATIVE']
//     side           — oneOf ['BUY','SELL']
//     quantity       — positive number
//     price          — positive number
//     tradeDate      — date
const schema = yup.object({
  tradeRef: yup
    .string()
    .matches(/^[A-Z]{3}-\d{8}-\d{4}$/, 'Format: AAA-YYYYMMDD-NNNN')
    .required('Trade reference is required'),

  instrumentId: yup
    .number()
    .typeError('Instrument ID must be a number')
    .integer()
    .positive()
    .required(),

  counterpartyId: yup
    .number()
    .typeError('Counterparty ID must be a number')
    .integer()
    .positive()
    .required(),

  assetClass: yup
    .string()
    .oneOf(['EQUITY', 'FX', 'BOND', 'DERIVATIVE'])
    .required(),

  side: yup
    .string()
    .oneOf(['BUY', 'SELL'])
    .required(),

  quantity: yup
    .number()
    .typeError('Quantity must be a number')
    .positive()
    .required(),

  price: yup
    .number()
    .typeError('Price must be a number')
    .positive()
    .required(),

  tradeDate: yup
    .date()
    .required('Trade date is required')
});
function AddTrade() {
  const { register, handleSubmit, formState: { errors, isSubmitting }, reset } =
        useForm({ resolver: yupResolver(schema) });

  async function onSubmit(values) {
  try {
    await api.createTrade(values);
    reset();
  } catch (err) {
    console.error(err);
    alert('Failed to create trade');
  }
}

  return (
    <section>
      <h2>Add trade</h2>
      <form onSubmit={handleSubmit(onSubmit)} className="trade-form">

  <label>
    Trade Ref
    <input
      {...register('tradeRef')}
      placeholder="EQU-20260603-0001"
    />
  </label>
  {errors.tradeRef && <p className="form-error">{errors.tradeRef.message}</p>}

  <label>
    Instrument ID
    <input type="number" {...register('instrumentId')} />
  </label>
  {errors.instrumentId && <p className="form-error">{errors.instrumentId.message}</p>}

  <label>
    Counterparty ID
    <input type="number" {...register('counterpartyId')} />
  </label>
  {errors.counterpartyId && <p className="form-error">{errors.counterpartyId.message}</p>}

  <label>
    Asset Class
    <select {...register('assetClass')}>
      <option value="">Select</option>
      <option value="EQUITY">EQUITY</option>
      <option value="FX">FX</option>
      <option value="BOND">BOND</option>
      <option value="DERIVATIVE">DERIVATIVE</option>
    </select>
  </label>
  {errors.assetClass && <p className="form-error">{errors.assetClass.message}</p>}

  <label>
    Side
    <select {...register('side')}>
      <option value="">Select</option>
      <option value="BUY">BUY</option>
      <option value="SELL">SELL</option>
    </select>
  </label>
  {errors.side && <p className="form-error">{errors.side.message}</p>}

  <label>
    Quantity
    <input
      type="number"
      step="0.0001"
      {...register('quantity')}
    />
  </label>
  {errors.quantity && <p className="form-error">{errors.quantity.message}</p>}

  <label>
    Price
    <input
      type="number"
      step="0.0001"
      {...register('price')}
    />
  </label>
  {errors.price && <p className="form-error">{errors.price.message}</p>}

  <label>
    Trade Date
    <input
      type="date"
      {...register('tradeDate')}
    />
  </label>
  {errors.tradeDate && <p className="form-error">{errors.tradeDate.message}</p>}

  <button
    type="submit"
    disabled={isSubmitting}
  >
    Submit
  </button>

</form>
    </section>
  );
}

export default withAuth(AddTrade);
