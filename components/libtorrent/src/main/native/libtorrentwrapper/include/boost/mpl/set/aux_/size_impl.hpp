
#ifndef BOOST_MPL_SET_AUX_SIZE_IMPL_HPP_INCLUDED
#define BOOST_MPL_SET_AUX_SIZE_IMPL_HPP_INCLUDED

// Copyright Aleksey Gurtovoy 2003-2004
//
// Distributed under the Boost Software License, Version 1.0. 
// (See accompanying file LICENSE_1_0.txt or copy at 
// http://www.boost.org/LICENSE_1_0.txt)
//
// See http://www.boost.org/libs/mpl for documentation.

// $Id: size_impl.hpp,v 1.2 2009-05-21 17:03:44 pvertenten Exp $
// $Date: 2009-05-21 17:03:44 $
// $Revision: 1.2 $

#include <boost/mpl/size_fwd.hpp>
#include <boost/mpl/set/aux_/tag.hpp>

namespace boost { namespace mpl {

template<>
struct size_impl< aux::set_tag >
{
    template< typename Set > struct apply
        : Set::size
    {
    };
};

}}

#endif // BOOST_MPL_SET_AUX_SIZE_IMPL_HPP_INCLUDED
