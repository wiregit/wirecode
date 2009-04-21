
#ifndef BOOST_MPL_AUX_POP_BACK_IMPL_HPP_INCLUDED
#define BOOST_MPL_AUX_POP_BACK_IMPL_HPP_INCLUDED

// Copyright Aleksey Gurtovoy 2000-2004
//
// Distributed under the Boost Software License, Version 1.0. 
// (See accompanying file LICENSE_1_0.txt or copy at 
// http://www.boost.org/LICENSE_1_0.txt)
//
// See http://www.boost.org/libs/mpl for documentation.

// $Id: pop_back_impl.hpp,v 1.1.2.1 2009-04-21 21:43:38 pvertenten Exp $
// $Date: 2009-04-21 21:43:38 $
// $Revision: 1.1.2.1 $

#include <boost/mpl/pop_back_fwd.hpp>
#include <boost/mpl/aux_/traits_lambda_spec.hpp>

namespace boost { namespace mpl {

// no default implementation; the definition is needed to make MSVC happy

template< typename Tag >
struct pop_back_impl
{
    template< typename Sequence > struct apply;
};

BOOST_MPL_ALGORITM_TRAITS_LAMBDA_SPEC(1, pop_back_impl)

}}

#endif // BOOST_MPL_AUX_POP_BACK_IMPL_HPP_INCLUDED
